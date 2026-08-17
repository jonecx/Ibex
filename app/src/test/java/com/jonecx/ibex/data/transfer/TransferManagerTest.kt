package com.jonecx.ibex.data.transfer

import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.fixtures.InMemoryProtocolHandler
import com.jonecx.ibex.fixtures.MEM_SCHEME
import com.jonecx.ibex.fixtures.RecordingAnalytics
import com.jonecx.ibex.fixtures.memFileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TransferManagerTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val handler = InMemoryProtocolHandler() // "mem:/" — the local device in these tests
    private val remoteHandler = InMemoryProtocolHandler("smb:/") // a pretend remote source
    private lateinit var journalFile: File
    private lateinit var appScope: CoroutineScope
    private lateinit var scheduler: RecordingScheduler
    private lateinit var recordingAnalytics: RecordingAnalytics
    private lateinit var manager: DefaultTransferManager

    @Before
    fun setup() {
        journalFile = File.createTempFile("mgr-journal", ".json").apply { delete() }
        appScope = CoroutineScope(dispatcher)
        scheduler = RecordingScheduler()
        recordingAnalytics = RecordingAnalytics(RuntimeEnvironment.getApplication())
        manager = DefaultTransferManager(
            scheduler = scheduler,
            engine = TransferEngine(setOf(handler, remoteHandler), dispatcher),
            journal = TransferJournal(journalFile, dispatcher),
            appScope = appScope,
            analytics = recordingAnalytics.manager,
        )
    }

    @After
    fun tearDown() {
        appScope.cancel()
        journalFile.delete()
        File(journalFile.parentFile, "${journalFile.name}.tmp").delete()
    }

    private fun seedSource(name: String, bytes: ByteArray) {
        handler.files["$MEM_SCHEME/src/$name"] = bytes
    }

    private fun sourceItem(name: String) = memFileItem("$MEM_SCHEME/src/$name", handler.files["$MEM_SCHEME/src/$name"]!!.size.toLong())

    @Test
    fun `enqueue_schedulesWorkerAndQueuesJob`() = runTest {
        seedSource("a.txt", "hi".toByteArray())

        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")

        assertEquals(1, scheduler.count)
        assertTrue(manager.snapshot.value.hasActive)
    }

    @Test
    fun `runQueue_copiesFilesAndPrunesOnComplete`() = runTest {
        seedSource("a.txt", "hello".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")

        manager.runQueue()

        assertArrayEquals("hello".toByteArray(), handler.files["$MEM_SCHEME/dest/a.txt"])
        assertFalse(manager.snapshot.value.hasActive)
    }

    @Test
    fun `runQueue_folderCopy_totalsDiscoveredFromCopyPassNotPreWalk`() = runTest {
        handler.dirs.add("$MEM_SCHEME/src/Folder")
        handler.files["$MEM_SCHEME/src/Folder/f1.txt"] = "one".toByteArray()
        handler.files["$MEM_SCHEME/src/Folder/f2.txt"] = "four".toByteArray()
        val seen = mutableListOf<TransferSnapshot>()
        val collector = launch(dispatcher) { manager.snapshot.collect { seen.add(it) } }

        manager.enqueue(
            listOf(memFileItem("$MEM_SCHEME/src/Folder", 0L, isDirectory = true)),
            ClipboardOperation.COPY,
            "$MEM_SCHEME/dest",
        )
        manager.runQueue()

        // The total is never pre-walked; it appears only once the copy pass has enumerated the tree (3 + 4).
        assertTrue(seen.any { it.totalBytes == 7L && it.totalFiles == 2 })
        assertArrayEquals("one".toByteArray(), handler.files["$MEM_SCHEME/dest/Folder/f1.txt"])
        assertArrayEquals("four".toByteArray(), handler.files["$MEM_SCHEME/dest/Folder/f2.txt"])
        collector.cancel()
    }

    @Test
    fun `runQueue_multipleSources_totalsAccumulateProgressively`() = runTest {
        seedSource("a.txt", "aa".toByteArray())
        seedSource("b.txt", "bbb".toByteArray())
        seedSource("c.txt", "cccc".toByteArray())
        val seen = mutableListOf<TransferSnapshot>()
        val collector = launch(dispatcher) { manager.snapshot.collect { seen.add(it) } }

        manager.enqueue(
            listOf(sourceItem("a.txt"), sourceItem("b.txt"), sourceItem("c.txt")),
            ClipboardOperation.COPY,
            "$MEM_SCHEME/dest",
        )
        manager.runQueue()

        // Totals grow as each top-level item is walked: partial totals appear before the full 2 + 3 + 4.
        assertTrue(seen.any { it.totalBytes == 2L && it.totalFiles == 1 })
        assertTrue(seen.any { it.totalBytes == 5L && it.totalFiles == 2 })
        assertTrue(seen.any { it.totalBytes == 9L && it.totalFiles == 3 })
        assertArrayEquals("cccc".toByteArray(), handler.files["$MEM_SCHEME/dest/c.txt"])
        collector.cancel()
    }

    @Test
    fun `runQueue_moveRemovesSource`() = runTest {
        seedSource("a.txt", "move".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.MOVE, "$MEM_SCHEME/dest")

        manager.runQueue()

        assertArrayEquals("move".toByteArray(), handler.files["$MEM_SCHEME/dest/a.txt"])
        assertNull(handler.files["$MEM_SCHEME/src/a.txt"])
    }

    @Test
    fun `cancel_queuedJob_isNeverCopied`() = runTest {
        seedSource("a.txt", "nope".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        val jobId = manager.snapshot.value.jobs.first().id

        manager.cancel(jobId)
        manager.runQueue()

        assertNull(handler.files["$MEM_SCHEME/dest/a.txt"])
        assertFalse(manager.snapshot.value.hasActive)
    }

    @Test
    fun `hasPendingWork_reflectsQueueState`() = runTest {
        assertFalse(manager.hasPendingWork())

        seedSource("a.txt", "x".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        assertTrue(manager.hasPendingWork())

        manager.runQueue()
        assertFalse(manager.hasPendingWork())
    }

    @Test
    fun `runQueue_emitsCompletionForDestination`() = runTest {
        seedSource("a.txt", "done".toByteArray())
        val completions = mutableListOf<String>()
        val job = launch(dispatcher) { manager.completions.collect { completions.add(it) } }
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")

        manager.runQueue()

        assertTrue(completions.contains("$MEM_SCHEME/dest"))
        job.cancel()
    }

    @Test
    fun `recoverAndResume_loadsJournalAndResumes`() = runTest {
        seedSource("a.txt", "resumed".toByteArray())
        // Pre-populate the journal as if a prior process left a queued job behind.
        TransferJournal(journalFile, dispatcher).save(
            listOf(
                TransferJob(
                    id = "job-1",
                    operation = ClipboardOperation.COPY,
                    sources = listOf(TransferSource("$MEM_SCHEME/src/a.txt", "a.txt", 7L, false)),
                    destinationDir = "$MEM_SCHEME/dest",
                    createdAt = 1L,
                    status = TransferStatus.QUEUED,
                ),
            ),
        )

        manager.recoverAndResume()
        assertEquals(1, scheduler.count)

        manager.runQueue()
        assertArrayEquals("resumed".toByteArray(), handler.files["$MEM_SCHEME/dest/a.txt"])
    }

    @Test
    fun `runQueue_crossHandlerMove_deletesSourceAfterFullCopy`() = runTest {
        remoteHandler.dirs.add("smb://src/Folder")
        remoteHandler.files["smb://src/Folder/f1.txt"] = "one".toByteArray()
        remoteHandler.files["smb://src/Folder/f2.txt"] = "two".toByteArray()
        val folder = memFileItem("smb://src/Folder", 0L, isDirectory = true)

        manager.enqueue(listOf(folder), ClipboardOperation.MOVE, "$MEM_SCHEME/dest")
        manager.runQueue()

        // Both files copied to the local destination.
        assertArrayEquals("one".toByteArray(), handler.files["$MEM_SCHEME/dest/Folder/f1.txt"])
        assertArrayEquals("two".toByteArray(), handler.files["$MEM_SCHEME/dest/Folder/f2.txt"])
        // Source removed only after the whole copy verified.
        assertNull(remoteHandler.files["smb://src/Folder/f1.txt"])
        assertNull(remoteHandler.files["smb://src/Folder/f2.txt"])
    }

    @Test
    fun `runQueue_crossHandlerMove_whenCopyFails_keepsSourceIntact`() = runTest {
        remoteHandler.dirs.add("smb://src/Folder")
        remoteHandler.files["smb://src/Folder/f1.txt"] = "one".toByteArray()
        remoteHandler.files["smb://src/Folder/f2.txt"] = "two".toByteArray()
        // The second file cannot be read: the move must fail and never delete the source.
        remoteHandler.failReadPaths.add("smb://src/Folder/f2.txt")
        val folder = memFileItem("smb://src/Folder", 0L, isDirectory = true)

        manager.enqueue(listOf(folder), ClipboardOperation.MOVE, "$MEM_SCHEME/dest")
        manager.runQueue()

        // No data loss: both source files remain.
        assertArrayEquals("one".toByteArray(), remoteHandler.files["smb://src/Folder/f1.txt"])
        assertArrayEquals("two".toByteArray(), remoteHandler.files["smb://src/Folder/f2.txt"])
    }

    @Test
    fun `runQueue_crossHandlerMove_whenListingFails_keepsSourceIntact`() = runTest {
        remoteHandler.dirs.add("smb://src/Folder")
        remoteHandler.files["smb://src/Folder/f1.txt"] = "one".toByteArray()
        // The folder cannot be enumerated: the move must fail, never treat it as empty and delete it.
        remoteHandler.failListPaths.add("smb://src/Folder")
        val folder = memFileItem("smb://src/Folder", 0L, isDirectory = true)

        manager.enqueue(listOf(folder), ClipboardOperation.MOVE, "$MEM_SCHEME/dest")
        manager.runQueue()

        // The job surfaces as failed (kept for retry), and the source is fully intact — no data loss.
        assertEquals(TransferStatus.FAILED, manager.snapshot.value.jobs.single().status)
        assertArrayEquals("one".toByteArray(), remoteHandler.files["smb://src/Folder/f1.txt"])
    }

    @Test
    fun `pause_queuedJob_isNotDrainedAndStaysPaused`() = runTest {
        seedSource("a.txt", "keep".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        val jobId = manager.snapshot.value.jobs.first().id

        manager.pause(jobId)
        assertEquals(TransferStatus.PAUSED, manager.snapshot.value.jobs.first().status)

        manager.runQueue()

        // A paused job is skipped by the queue and left untouched, temp and all.
        assertNull(handler.files["$MEM_SCHEME/dest/a.txt"])
        assertEquals(TransferStatus.PAUSED, manager.snapshot.value.jobs.first().status)
        assertTrue(manager.snapshot.value.hasActive)
        assertFalse(manager.hasPendingWork())
    }

    @Test
    fun `resume_pausedJob_reschedulesAndCompletes`() = runTest {
        seedSource("a.txt", "done".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        val jobId = manager.snapshot.value.jobs.first().id
        manager.pause(jobId)

        manager.resume(jobId)
        assertEquals(TransferStatus.QUEUED, manager.snapshot.value.jobs.first().status)
        // enqueue scheduled once, resume schedules again.
        assertEquals(2, scheduler.count)

        manager.runQueue()
        assertArrayEquals("done".toByteArray(), handler.files["$MEM_SCHEME/dest/a.txt"])
        assertFalse(manager.snapshot.value.hasActive)
    }

    @Test
    fun `pauseAll_pausesEveryQueuedJob`() = runTest {
        seedSource("a.txt", "a".toByteArray())
        seedSource("b.txt", "b".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        manager.enqueue(listOf(sourceItem("b.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")

        manager.pauseAll()

        assertTrue(manager.snapshot.value.jobs.all { it.status == TransferStatus.PAUSED })
        manager.runQueue()
        assertNull(handler.files["$MEM_SCHEME/dest/a.txt"])
        assertNull(handler.files["$MEM_SCHEME/dest/b.txt"])
    }

    @Test
    fun `pausedJob_isSkipped_whileOtherJobRuns`() = runTest {
        seedSource("a.txt", "a".toByteArray())
        seedSource("b.txt", "b".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        manager.enqueue(listOf(sourceItem("b.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        val firstId = manager.snapshot.value.jobs.first().id

        manager.pause(firstId)
        manager.runQueue()

        // The queued sibling runs to completion; the paused job is left waiting.
        assertNull(handler.files["$MEM_SCHEME/dest/a.txt"])
        assertArrayEquals("b".toByteArray(), handler.files["$MEM_SCHEME/dest/b.txt"])
        assertEquals(TransferStatus.PAUSED, manager.snapshot.value.jobs.single().status)
    }

    @Test
    fun `pausedJob_survivesReloadAndIsNotAutoResumed`() = runTest {
        seedSource("a.txt", "later".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        val jobId = manager.snapshot.value.jobs.first().id
        manager.pause(jobId)

        // A fresh manager over the same journal simulates a process restart / reboot.
        val restartScheduler = RecordingScheduler()
        val restarted = DefaultTransferManager(
            scheduler = restartScheduler,
            engine = TransferEngine(setOf(handler, remoteHandler), dispatcher),
            journal = TransferJournal(journalFile, dispatcher),
            appScope = appScope,
            analytics = recordingAnalytics.manager,
        )

        restarted.recoverAndResume()

        // Paused work is remembered but never auto-resumed.
        assertEquals(0, restartScheduler.count)
        assertEquals(TransferStatus.PAUSED, restarted.snapshot.value.jobs.single().status)
        assertFalse(restarted.hasPendingWork())
    }

    @Test
    fun `pause_midCopy_checkpointsProgressAndKeepsTempForResume`() = runTest {
        val data = ByteArray(200_000) { it.toByte() }
        // A handler that flips the job to paused as soon as the first chunk is read.
        val pausingHandler = object : InMemoryProtocolHandler() {
            var onRead: (() -> Unit)? = null
            override suspend fun openInputStream(path: String, offset: Long): InputStream {
                val base = super.openInputStream(path, offset)
                return object : InputStream() {
                    override fun read(): Int = base.read()
                    override fun read(b: ByteArray): Int {
                        onRead?.invoke()
                        return base.read(b)
                    }
                    override fun close() = base.close()
                }
            }
        }
        pausingHandler.files["mem://src/big.bin"] = data
        val pausingJournal = File.createTempFile("pausing-journal", ".json").apply { delete() }
        val mgr = DefaultTransferManager(
            scheduler = RecordingScheduler(),
            engine = TransferEngine(setOf(pausingHandler), dispatcher),
            journal = TransferJournal(pausingJournal, dispatcher),
            appScope = appScope,
            analytics = recordingAnalytics.manager,
        )
        mgr.enqueue(listOf(memFileItem("mem://src/big.bin", data.size.toLong())), ClipboardOperation.COPY, "mem://dest")
        val jobId = mgr.snapshot.value.jobs.first().id
        pausingHandler.onRead = { mgr.pause(jobId) }

        mgr.runQueue()

        val job = mgr.snapshot.value.jobs.single()
        assertEquals(TransferStatus.PAUSED, job.status)
        // Progress checkpointed partway, the temp kept, and the final never promoted.
        assertTrue(job.bytesDone in 1 until data.size.toLong())
        assertTrue((pausingHandler.files["mem://dest/big.bin.ibexpart"]?.size ?: 0) > 0)
        assertNull(pausingHandler.files["mem://dest/big.bin"])
        // A pause is not a terminal outcome, so no completion event is emitted.
        assertFalse(recordingAnalytics.eventNames().contains("transfer_complete"))
        pausingJournal.delete()
    }

    @Test
    fun `failedJob_isKeptForRetry_notPruned`() = runTest {
        seedSource("a.txt", "data".toByteArray())
        handler.failReadPaths.add("$MEM_SCHEME/src/a.txt")
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")

        manager.runQueue()

        assertEquals(TransferStatus.FAILED, manager.snapshot.value.jobs.single().status)
        assertTrue(manager.snapshot.value.hasFailed)
    }

    @Test
    fun `retry_failedJob_requeuesAndCompletes`() = runTest {
        seedSource("a.txt", "data".toByteArray())
        handler.failReadPaths.add("$MEM_SCHEME/src/a.txt")
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        manager.runQueue()
        val jobId = manager.snapshot.value.jobs.single().id

        handler.failReadPaths.clear()
        manager.retry(jobId)
        manager.runQueue()

        assertArrayEquals("data".toByteArray(), handler.files["$MEM_SCHEME/dest/a.txt"])
        assertFalse(manager.snapshot.value.hasActive)
    }

    @Test
    fun `dismiss_failedJob_removesIt`() = runTest {
        seedSource("a.txt", "data".toByteArray())
        handler.failReadPaths.add("$MEM_SCHEME/src/a.txt")
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        manager.runQueue()
        val jobId = manager.snapshot.value.jobs.single().id

        manager.dismiss(jobId)

        assertTrue(manager.snapshot.value.jobs.isEmpty())
        assertFalse(manager.snapshot.value.hasActive)
    }

    @Test
    fun `failedJob_survivesReloadAndIsNotAutoResumed`() = runTest {
        seedSource("a.txt", "data".toByteArray())
        handler.failReadPaths.add("$MEM_SCHEME/src/a.txt")
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        manager.runQueue()

        val restartScheduler = RecordingScheduler()
        val restarted = DefaultTransferManager(
            scheduler = restartScheduler,
            engine = TransferEngine(setOf(handler, remoteHandler), dispatcher),
            journal = TransferJournal(journalFile, dispatcher),
            appScope = appScope,
            analytics = recordingAnalytics.manager,
        )
        restarted.recoverAndResume()

        assertEquals(0, restartScheduler.count)
        assertEquals(TransferStatus.FAILED, restarted.snapshot.value.jobs.single().status)
        assertFalse(restarted.hasPendingWork())
    }

    @Test
    fun `runQueue_overwritePolicy_replacesExistingDestination`() = runTest {
        handler.files["$MEM_SCHEME/dest/a.txt"] = "old".toByteArray()
        seedSource("a.txt", "new".toByteArray())
        manager.enqueue(
            listOf(sourceItem("a.txt")),
            ClipboardOperation.COPY,
            "$MEM_SCHEME/dest",
            ConflictPolicy.OVERWRITE,
        )

        manager.runQueue()

        assertArrayEquals("new".toByteArray(), handler.files["$MEM_SCHEME/dest/a.txt"])
        assertNull(handler.files["$MEM_SCHEME/dest/a (1).txt"])
    }

    @Test
    fun `runQueue_completedCopy_emitsSuccessCompletionEvent`() = runTest {
        seedSource("a.txt", "hello".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")

        manager.runQueue()

        val event = recordingAnalytics.event("transfer_complete")
        assertNotNull(event)
        assertEquals("copy", event?.get("operation"))
        assertEquals("success", event?.get("result"))
        assertEquals(false, event?.get("is_remote"))
        assertEquals(1, event?.get("item_count"))
        assertEquals(5L, event?.get("size_bytes"))
        assertTrue(event?.containsKey("duration_ms") == true)
        // A local job stays off the remote QoE metric.
        assertNull(recordingAnalytics.metric("file_transfer_complete"))
    }

    @Test
    fun `runQueue_failedJob_emitsFailureCompletionEvent`() = runTest {
        seedSource("a.txt", "data".toByteArray())
        handler.failReadPaths.add("$MEM_SCHEME/src/a.txt")
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")

        manager.runQueue()

        val event = recordingAnalytics.event("transfer_complete")
        assertNotNull(event)
        assertEquals("failure", event?.get("result"))
        assertEquals("copy", event?.get("operation"))
    }

    @Test
    fun `runQueue_remoteMove_alsoEmitsCompletionQoeMetric`() = runTest {
        remoteHandler.dirs.add("smb://src/Folder")
        remoteHandler.files["smb://src/Folder/f1.txt"] = "one".toByteArray()
        val folder = memFileItem("smb://src/Folder", 0L, isDirectory = true)
        manager.enqueue(listOf(folder), ClipboardOperation.MOVE, "$MEM_SCHEME/dest")

        manager.runQueue()

        val event = recordingAnalytics.event("transfer_complete")
        assertEquals(true, event?.get("is_remote"))
        assertEquals("move", event?.get("operation"))
        // Remote jobs also surface a QoE metric so throughput is queryable in Axiom.
        val metric = recordingAnalytics.metric("file_transfer_complete")
        assertNotNull(metric)
        assertEquals("success", metric?.get("result"))
    }

    @Test
    fun `resumedJob_completion_emitsSingleSuccessEventAfterPause`() = runTest {
        val data = ByteArray(200_000) { it.toByte() }
        val pausingHandler = object : InMemoryProtocolHandler() {
            var onRead: (() -> Unit)? = null
            override suspend fun openInputStream(path: String, offset: Long): InputStream {
                val base = super.openInputStream(path, offset)
                return object : InputStream() {
                    override fun read(): Int = base.read()
                    override fun read(b: ByteArray): Int {
                        onRead?.invoke()
                        return base.read(b)
                    }
                    override fun close() = base.close()
                }
            }
        }
        pausingHandler.files["mem://src/big.bin"] = data
        val pausingJournal = File.createTempFile("resume-journal", ".json").apply { delete() }
        val mgr = DefaultTransferManager(
            scheduler = RecordingScheduler(),
            engine = TransferEngine(setOf(pausingHandler), dispatcher),
            journal = TransferJournal(pausingJournal, dispatcher),
            appScope = appScope,
            analytics = recordingAnalytics.manager,
        )
        mgr.enqueue(listOf(memFileItem("mem://src/big.bin", data.size.toLong())), ClipboardOperation.COPY, "mem://dest")
        val jobId = mgr.snapshot.value.jobs.first().id

        // First run pauses mid-copy: no completion event yet.
        pausingHandler.onRead = { mgr.pause(jobId) }
        mgr.runQueue()
        assertFalse(recordingAnalytics.eventNames().contains("transfer_complete"))

        // Resume and finish: exactly one success event fires, carrying the summed duration.
        pausingHandler.onRead = null
        mgr.resume(jobId)
        mgr.runQueue()

        assertArrayEquals(data, pausingHandler.files["mem://dest/big.bin"])
        assertEquals(1, recordingAnalytics.eventNames().count { it == "transfer_complete" })
        val event = recordingAnalytics.event("transfer_complete")
        assertEquals("success", event?.get("result"))
        assertEquals(data.size.toLong(), event?.get("size_bytes"))
        assertTrue(event?.containsKey("duration_ms") == true)
        pausingJournal.delete()
    }

    private class RecordingScheduler : TransferScheduler {
        var count = 0
        override fun ensureRunning() {
            count += 1
        }
    }
}
