package com.jonecx.ibex.data.transfer

import androidx.compose.runtime.Immutable
import com.jonecx.ibex.data.repository.ClipboardOperation
import kotlinx.serialization.Serializable

// Whole seconds left at the current rate, or -1 when it cannot be estimated. Shared by the per-job and
// aggregate ETAs so the estimate is computed one way only.
internal fun etaSeconds(totalBytes: Long, bytesDone: Long, bytesPerSecond: Long): Long =
    if (bytesPerSecond > 0L && totalBytes > bytesDone) (totalBytes - bytesDone) / bytesPerSecond else -1L

enum class TransferStatus {
    QUEUED,
    RUNNING,

    // User-paused: the .ibexpart temp is kept so resume picks up from the last byte. Not auto-resumed on reboot.
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
}

// One top-level item the user picked. The tree under a directory is re-walked at run time,
// so the journal stays tiny even for a folder holding 100k files.
@Serializable
data class TransferSource(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
)

// Durable record of one paste. Persisted to the journal so a transfer survives process death
// and reboot; totals/checkpoints are coarse (updated on file boundaries), never per-byte.
@Serializable
data class TransferJob(
    val id: String,
    val operation: ClipboardOperation,
    val sources: List<TransferSource>,
    val destinationDir: String,
    val createdAt: Long,
    val status: TransferStatus = TransferStatus.QUEUED,
    val totalBytes: Long = 0L,
    val totalFiles: Int = 0,
    val bytesDone: Long = 0L,
    val filesDone: Int = 0,
    // Any source or the destination is remote (smb://): the worker holds a WifiLock while it runs.
    val touchesRemote: Boolean = false,
)

// Live, in-memory view of a job for the UI. Fast-moving fields (currentFile*, bytesPerSecond)
// live here only and never hit disk, so the progress bar animates without I/O churn.
@Immutable
data class TransferProgress(
    val id: String,
    val operation: ClipboardOperation,
    val destinationDir: String,
    val status: TransferStatus,
    val totalBytes: Long,
    val totalFiles: Int,
    val bytesDone: Long,
    val filesDone: Int,
    val currentFileName: String? = null,
    val bytesPerSecond: Long = 0L,
    // Progress of the file being copied right now, for the sheet's per-file bar. Live-only, never journaled.
    val currentFileBytes: Long = 0L,
    val currentFileTotal: Long = 0L,
    // Top-level items the user picked, so a still-unmeasured queued job can show "N items" honestly.
    val itemCount: Int = 0,
) {
    // 0 total means "not counted yet": the bar shows indeterminate until enumeration finishes.
    val fraction: Float get() = if (totalBytes > 0L) (bytesDone.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val currentFileFraction: Float
        get() = if (currentFileTotal > 0L) (currentFileBytes.toFloat() / currentFileTotal).coerceIn(0f, 1f) else 0f
    val isCounting: Boolean get() = status == TransferStatus.RUNNING && totalBytes == 0L
    val isPaused: Boolean get() = status == TransferStatus.PAUSED
    val isFailed: Boolean get() = status == TransferStatus.FAILED
    val secondsRemaining: Long get() = etaSeconds(totalBytes, bytesDone, bytesPerSecond)
}

@Immutable
data class TransferSnapshot(
    val jobs: List<TransferProgress> = emptyList(),
) {
    // Non-terminal jobs: what the bar and sheet actually show. Excludes COMPLETED/CANCELLED (already pruned).
    private val active: List<TransferProgress> get() = jobs.filter {
        it.status == TransferStatus.RUNNING ||
            it.status == TransferStatus.QUEUED ||
            it.status == TransferStatus.PAUSED ||
            it.status == TransferStatus.FAILED
    }

    // The bar is visible whenever there is any non-terminal job, paused or failed included.
    val hasActive: Boolean get() = active.isNotEmpty()
    val activeCount: Int get() = active.size

    // Something is running or waiting to run: gates "Pause all" and whether the worker should keep going.
    val hasRunningOrQueued: Boolean get() = jobs.any {
        it.status == TransferStatus.RUNNING || it.status == TransferStatus.QUEUED
    }
    val hasFailed: Boolean get() = jobs.any { it.status == TransferStatus.FAILED }

    // Icon/verb for the collapsed row: prefer the running job, else the next queued, else a paused one.
    val primaryOperation: ClipboardOperation? get() = (
        jobs.firstOrNull { it.status == TransferStatus.RUNNING }
            ?: jobs.firstOrNull { it.status == TransferStatus.QUEUED }
            ?: jobs.firstOrNull { it.status == TransferStatus.PAUSED }
        )?.operation

    // Aggregate over jobs that carry real progress (paused ones freeze their bytes, so they still count);
    // FAILED is excluded so a failure never drags the headline bar. Speed drops on its own as paused
    // jobs clear their live bytes-per-second.
    private val counted: List<TransferProgress> get() = jobs.filter {
        it.status == TransferStatus.RUNNING ||
            it.status == TransferStatus.QUEUED ||
            it.status == TransferStatus.PAUSED
    }
    val totalBytes: Long get() = counted.sumOf { it.totalBytes }
    val bytesDone: Long get() = counted.sumOf { it.bytesDone }
    val totalFiles: Int get() = counted.sumOf { it.totalFiles }
    val filesDone: Int get() = counted.sumOf { it.filesDone }
    val bytesPerSecond: Long get() = counted.sumOf { it.bytesPerSecond }
    val fraction: Float get() = if (totalBytes > 0L) (bytesDone.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val secondsRemaining: Long get() = etaSeconds(totalBytes, bytesDone, bytesPerSecond)

    // Indeterminate only while something is actively running before its size is known ("Preparing…").
    val isCounting: Boolean get() = hasRunningOrQueued && totalBytes == 0L
}
