package com.jonecx.ibex.data.transfer

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.util.FileTypeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

interface TransferManager {
    val snapshot: StateFlow<TransferSnapshot>

    // Emits the destination dir of a job that just finished, so a screen showing that folder can refresh.
    val completions: SharedFlow<String>

    fun enqueue(
        files: List<FileItem>,
        operation: ClipboardOperation,
        destinationDir: String,
        conflictPolicy: ConflictPolicy = ConflictPolicy.AUTO,
    )
    fun cancel(jobId: String)
    fun cancelAll()

    // Pause one job (keeps its .ibexpart temp) / resume it from that temp / pause every running or queued job.
    fun pause(jobId: String)
    fun resume(jobId: String)
    fun pauseAll()

    // Requeue a FAILED job to resume from its temp / drop a FAILED job the user has acknowledged.
    fun retry(jobId: String)
    fun dismiss(jobId: String)

    // Loads the journal on app start and resumes anything left unfinished (auto-resume).
    fun recoverAndResume()

    // True while any pending/running job touches SMB, so the worker holds a WifiLock only when needed.
    fun hasRemoteWork(): Boolean

    // Loads the journal (once) then reports whether anything is left to do, so the worker can skip
    // spinning up a foreground service for an empty queue.
    suspend fun hasPendingWork(): Boolean

    // Drains QUEUED jobs one at a time. Called only by TransferWorker inside its foreground window.
    suspend fun runQueue()
}

class DefaultTransferManager(
    private val scheduler: TransferScheduler,
    private val engine: TransferEngine,
    private val journal: TransferJournal,
    private val appScope: CoroutineScope,
) : TransferManager {

    private companion object {
        const val EMIT_THROTTLE_MS = 200L
        const val CHECKPOINT_THROTTLE_MS = 5_000L
        const val SPEED_SMOOTHING = 0.4
    }

    private data class Live(
        val bytesDone: Long = 0L,
        val filesDone: Int = 0,
        val currentFileName: String? = null,
        val bytesPerSecond: Long = 0L,
        val currentFileBytes: Long = 0L,
        val currentFileTotal: Long = 0L,
    )

    private val jobsState = MutableStateFlow<List<TransferJob>>(emptyList())
    private val liveState = MutableStateFlow<Map<String, Live>>(emptyMap())
    private val cancelledIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // Jobs the user paused. Polled by the engine listener (running job) and checked in runQueue (queued job).
    private val pausedIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val _completions = MutableSharedFlow<String>(extraBufferCapacity = 8)
    override val completions: SharedFlow<String> = _completions.asSharedFlow()

    @Volatile
    private var loaded = false
    private val loadMutex = Mutex()

    // Populate the in-memory queue from the journal exactly once, mapping interrupted work back to QUEUED.
    // Called by every entry point so a reboot-triggered worker resumes even before the UI is created.
    private suspend fun ensureLoaded() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            jobsState.value = journal.load()
                .map { if (it.status == TransferStatus.RUNNING) it.copy(status = TransferStatus.QUEUED) else it }
                .filter {
                    it.status == TransferStatus.QUEUED ||
                        it.status == TransferStatus.PAUSED ||
                        it.status == TransferStatus.FAILED
                }
            // A job that was paused before a kill/reboot stays paused (never auto-resumed); re-arm its flag.
            jobsState.value.filter { it.status == TransferStatus.PAUSED }.forEach { pausedIds.add(it.id) }
            loaded = true
        }
    }

    override val snapshot: StateFlow<TransferSnapshot> =
        combine(jobsState, liveState) { jobs, live ->
            TransferSnapshot(jobs.map { it.toProgress(live[it.id]) })
        }.stateIn(appScope, SharingStarted.Eagerly, TransferSnapshot())

    override fun enqueue(
        files: List<FileItem>,
        operation: ClipboardOperation,
        destinationDir: String,
        conflictPolicy: ConflictPolicy,
    ) {
        if (files.isEmpty()) return
        val job = TransferJob(
            id = UUID.randomUUID().toString(),
            operation = operation,
            sources = files.map { TransferSource(it.path, it.name, it.size, it.isDirectory) },
            destinationDir = destinationDir,
            createdAt = System.currentTimeMillis(),
            touchesRemote = destinationDir.startsWith(FileTypeUtils.SMB_SCHEME_PREFIX) ||
                files.any { it.path.startsWith(FileTypeUtils.SMB_SCHEME_PREFIX) },
            conflictPolicy = conflictPolicy,
        )
        appScope.launch {
            ensureLoaded()
            jobsState.update { it + job }
            journal.save(jobsState.value)
            scheduler.ensureRunning()
        }
    }

    override fun cancel(jobId: String) {
        cancelledIds.add(jobId)
        // A still-queued job never reaches the engine, so mark it here; a running one is marked by runJob.
        jobsState.update { jobs ->
            jobs.map { if (it.id == jobId && it.status == TransferStatus.QUEUED) it.copy(status = TransferStatus.CANCELLED) else it }
        }
        appScope.launch { pruneFinished() }
    }

    override fun cancelAll() {
        jobsState.value.forEach { cancelledIds.add(it.id) }
        jobsState.update { jobs ->
            jobs.map {
                if (it.status == TransferStatus.QUEUED ||
                    it.status == TransferStatus.PAUSED ||
                    it.status == TransferStatus.FAILED
                ) {
                    it.copy(status = TransferStatus.CANCELLED)
                } else {
                    it
                }
            }
        }
        appScope.launch { pruneFinished() }
    }

    override fun pause(jobId: String) {
        pausedIds.add(jobId)
        // A running job is flipped to PAUSED by runJob's catch; a still-queued one never reaches the engine,
        // so mark it here. A checkpoint of its bytes/files is already persisted; the temp stays for resume.
        updateJob(jobId) { if (it.status == TransferStatus.QUEUED) it.copy(status = TransferStatus.PAUSED) else it }
        appScope.launch { journal.save(jobsState.value) }
    }

    override fun resume(jobId: String) {
        pausedIds.remove(jobId)
        cancelledIds.remove(jobId)
        updateJob(jobId) { if (it.status == TransferStatus.PAUSED) it.copy(status = TransferStatus.QUEUED) else it }
        appScope.launch {
            journal.save(jobsState.value)
            // The engine skips already-finished files and continues from the .ibexpart temp.
            if (jobsState.value.any { it.status == TransferStatus.QUEUED }) scheduler.ensureRunning()
        }
    }

    override fun pauseAll() {
        jobsState.value.forEach {
            if (it.status == TransferStatus.QUEUED || it.status == TransferStatus.RUNNING) pausedIds.add(it.id)
        }
        updateJob { if (it.status == TransferStatus.QUEUED) it.copy(status = TransferStatus.PAUSED) else it }
        appScope.launch { journal.save(jobsState.value) }
    }

    override fun retry(jobId: String) {
        // A failed run left its .ibexpart temp behind, so requeuing resumes from the last verified byte.
        cancelledIds.remove(jobId)
        pausedIds.remove(jobId)
        updateJob(jobId) { if (it.status == TransferStatus.FAILED) it.copy(status = TransferStatus.QUEUED) else it }
        appScope.launch {
            journal.save(jobsState.value)
            if (jobsState.value.any { it.status == TransferStatus.QUEUED }) scheduler.ensureRunning()
        }
    }

    override fun dismiss(jobId: String) {
        // Drop a failure the user has acknowledged; the leftover temp is theirs to re-paste later if wanted.
        cancelledIds.remove(jobId)
        pausedIds.remove(jobId)
        jobsState.update { jobs -> jobs.filterNot { it.id == jobId && it.status == TransferStatus.FAILED } }
        appScope.launch { journal.save(jobsState.value) }
    }

    override fun recoverAndResume() {
        appScope.launch {
            ensureLoaded()
            // The engine skips already-finished files and resumes the rest, so just restart the worker.
            if (jobsState.value.any { it.status == TransferStatus.QUEUED }) scheduler.ensureRunning()
        }
    }

    override fun hasRemoteWork(): Boolean = jobsState.value.any {
        it.touchesRemote && (it.status == TransferStatus.QUEUED || it.status == TransferStatus.RUNNING)
    }

    override suspend fun hasPendingWork(): Boolean {
        ensureLoaded()
        return jobsState.value.any {
            it.status == TransferStatus.QUEUED || it.status == TransferStatus.RUNNING
        }
    }

    override suspend fun runQueue() {
        ensureLoaded()
        while (true) {
            val job = jobsState.value.firstOrNull { it.status == TransferStatus.QUEUED } ?: break
            if (job.id in cancelledIds) {
                setStatus(job.id, TransferStatus.CANCELLED)
                pruneFinished()
                continue
            }
            runJob(job)
        }
    }

    private suspend fun runJob(job: TransferJob) {
        setStatus(job.id, TransferStatus.RUNNING)
        liveState.update { it + (job.id to Live()) }

        val runState = RunState()
        val listener = object : TransferListener {
            override fun isCancelled(): Boolean = job.id in cancelledIds
            override fun isPaused(): Boolean = job.id in pausedIds
            override suspend fun onFileStart(name: String, size: Long) = runState.onFileStart(job, name, size)
            override suspend fun onBytes(name: String, delta: Long) = runState.onBytes(job, name, delta)
            override suspend fun onFileComplete() = runState.onFileComplete(job)
        }

        try {
            // The count is produced by the copy pass itself (engine.transfer returns what it walked), so a
            // remote directory is listed once, not measured and then copied. Totals stay 0 until a source's
            // subtree is walked, so the bar shows "Preparing…" (isCounting) until then. A listing failure
            // surfaces from the copy as a normal job failure, never as a phantom empty tree.
            val copiedSources = mutableListOf<TransferSource>()
            var measuredFiles = 0
            var measuredBytes = 0L
            for (source in job.sources) {
                if (job.id in cancelledIds) throw TransferCancelledException()
                if (job.id in pausedIds) throw TransferPausedException()
                val outcome = engine.transfer(source, job.destinationDir, job.operation, job.conflictPolicy, listener)
                measuredFiles += outcome.measured.files
                measuredBytes += outcome.measured.bytes
                if (!outcome.relocated) copiedSources.add(source)
                // Firm up the total as each top-level item's subtree is counted; the bar leaves "Preparing…" then.
                updateJob(job.id) { it.copy(totalFiles = measuredFiles, totalBytes = measuredBytes) }
            }
            journal.save(jobsState.value)

            // Only now, with every source copied without error, remove the originals for a MOVE — and only
            // if the byte/file counts prove the copy is actually complete. A partial copy keeps the source.
            if (job.operation == ClipboardOperation.MOVE && copiedSources.isNotEmpty()) {
                val complete = runState.filesDone >= measuredFiles && runState.bytesDone >= measuredBytes
                if (!complete) throw IOException("Copy incomplete; keeping sources")
                copiedSources.forEach { source ->
                    // A delete stumble after a verified copy is not data loss (the bytes are safely at the
                    // destination); log it, don't fail an otherwise-successful move.
                    runCatching { engine.deleteSource(source) }
                        .onFailure { Timber.e(it, "Failed to remove moved source after copy") }
                }
            }
            setStatus(job.id, TransferStatus.COMPLETED)
            journal.save(jobsState.value)
            emitCompletions(job)
            pruneFinished()
        } catch (e: TransferCancelledException) {
            finishCancelled(job.id)
        } catch (e: TransferPausedException) {
            // User paused this job: checkpoint progress, hold it PAUSED, keep its temp. Not pruned, not resumed
            // until resume() flips it back to QUEUED. The worker moves on to any other queued job.
            updateJob(job.id) {
                it.copy(status = TransferStatus.PAUSED, bytesDone = runState.bytesDone, filesDone = runState.filesDone)
            }
            journal.save(jobsState.value)
        } catch (e: CancellationException) {
            // Whole worker was stopped (system, reboot). Checkpoint progress and leave the job QUEUED to resume.
            updateJob(job.id) {
                it.copy(status = TransferStatus.QUEUED, bytesDone = runState.bytesDone, filesDone = runState.filesDone)
            }
            journal.save(jobsState.value)
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Transfer job failed: ${job.id}")
            setStatus(job.id, TransferStatus.FAILED)
            journal.save(jobsState.value)
            // Keep the FAILED job (and its .ibexpart temp) so the sheet can offer a retry; pruneFinished
            // clears only COMPLETED/CANCELLED.
            pruneFinished()
        } finally {
            liveState.update { it - job.id }
        }
    }

    // Per-job accumulator. The engine runs one file at a time on one coroutine, so plain vars are safe here.
    private inner class RunState {
        var bytesDone = 0L
            private set
        var filesDone = 0
            private set
        private var currentName: String? = null
        private var currentFileBytes = 0L
        private var currentFileTotal = 0L
        private var lastEmitMs = 0L
        private var lastCheckpointMs = System.currentTimeMillis()
        private var bytesSinceEmit = 0L
        private var smoothedBps = 0.0

        // A new leaf file begins: reset its per-file counter and record its size for the sheet's file bar.
        fun onFileStart(job: TransferJob, name: String, size: Long) {
            currentName = name
            currentFileBytes = 0L
            currentFileTotal = size
            pushLive(job)
        }

        suspend fun onBytes(job: TransferJob, name: String, delta: Long) {
            bytesDone += delta
            bytesSinceEmit += delta
            currentName = name
            currentFileBytes += delta
            val now = System.currentTimeMillis()
            val elapsed = now - lastEmitMs
            if (elapsed < EMIT_THROTTLE_MS) return
            val instantBps = if (elapsed > 0) bytesSinceEmit * 1000.0 / elapsed else 0.0
            smoothedBps = if (smoothedBps == 0.0) {
                instantBps
            } else {
                SPEED_SMOOTHING * instantBps + (1 - SPEED_SMOOTHING) * smoothedBps
            }
            pushLive(job)
            lastEmitMs = now
            bytesSinceEmit = 0L
        }

        suspend fun onFileComplete(job: TransferJob) {
            filesDone += 1
            pushLive(job)
            val now = System.currentTimeMillis()
            if (now - lastCheckpointMs >= CHECKPOINT_THROTTLE_MS) {
                updateJob(job.id) { it.copy(bytesDone = bytesDone, filesDone = filesDone) }
                journal.save(jobsState.value)
                lastCheckpointMs = now
            }
        }

        private fun pushLive(job: TransferJob) {
            val live = Live(
                bytesDone = bytesDone,
                filesDone = filesDone,
                currentFileName = currentName,
                bytesPerSecond = smoothedBps.toLong(),
                currentFileBytes = currentFileBytes,
                currentFileTotal = currentFileTotal,
            )
            liveState.update { it + (job.id to live) }
        }
    }

    private fun TransferJob.toProgress(live: Live?): TransferProgress = TransferProgress(
        id = id,
        operation = operation,
        destinationDir = destinationDir,
        status = status,
        totalBytes = totalBytes,
        totalFiles = totalFiles,
        bytesDone = live?.bytesDone ?: bytesDone,
        filesDone = live?.filesDone ?: filesDone,
        currentFileName = live?.currentFileName,
        bytesPerSecond = live?.bytesPerSecond ?: 0L,
        currentFileBytes = live?.currentFileBytes ?: 0L,
        currentFileTotal = live?.currentFileTotal ?: 0L,
        itemCount = sources.size,
    )

    // Nudge any live screen to re-list: the destination for every job, plus the source folder(s) for a
    // move (the folder the user was browsing when they cut). Matching is done by the collector in the VM.
    private fun emitCompletions(job: TransferJob) {
        _completions.tryEmit(job.destinationDir)
        if (job.operation == ClipboardOperation.MOVE) {
            job.sources
                .map { it.path.trimEnd('/').substringBeforeLast('/') }
                .distinct()
                .forEach { _completions.tryEmit(it) }
        }
    }

    private suspend fun finishCancelled(jobId: String) {
        setStatus(jobId, TransferStatus.CANCELLED)
        journal.save(jobsState.value)
        pruneFinished()
    }

    private fun setStatus(jobId: String, status: TransferStatus) =
        updateJob(jobId) { it.copy(status = status) }

    private fun updateJob(jobId: String, transform: (TransferJob) -> TransferJob) {
        jobsState.update { jobs -> jobs.map { if (it.id == jobId) transform(it) else it } }
    }

    // Transform every job (used by the "all" bulk operations).
    private fun updateJob(transform: (TransferJob) -> TransferJob) {
        jobsState.update { jobs -> jobs.map(transform) }
    }

    // Drop terminal jobs from the live queue + journal so the bar reflects only in-flight work. PAUSED and
    // FAILED are not terminal here (both wait on the user), so they stay put with their temps.
    private suspend fun pruneFinished() {
        val remaining = jobsState.value.filter {
            it.status == TransferStatus.QUEUED ||
                it.status == TransferStatus.RUNNING ||
                it.status == TransferStatus.PAUSED ||
                it.status == TransferStatus.FAILED
        }
        if (remaining.size != jobsState.value.size) {
            jobsState.update { remaining }
            journal.save(remaining)
        }
        cancelledIds.retainAll(remaining.map { it.id }.toSet())
    }
}
