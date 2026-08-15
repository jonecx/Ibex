package com.jonecx.ibex.data.transfer

import androidx.compose.runtime.Immutable
import com.jonecx.ibex.data.repository.ClipboardOperation
import kotlinx.serialization.Serializable

enum class TransferStatus {
    QUEUED,
    RUNNING,
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
) {
    // 0 total means "not counted yet": the bar shows indeterminate until enumeration finishes.
    val fraction: Float get() = if (totalBytes > 0L) (bytesDone.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val isCounting: Boolean get() = status == TransferStatus.RUNNING && totalBytes == 0L
}

@Immutable
data class TransferSnapshot(
    val jobs: List<TransferProgress> = emptyList(),
) {
    val hasActive: Boolean get() = jobs.any {
        it.status == TransferStatus.RUNNING || it.status == TransferStatus.QUEUED
    }
    val activeCount: Int get() = jobs.count {
        it.status == TransferStatus.RUNNING || it.status == TransferStatus.QUEUED
    }
    val totalBytes: Long get() = jobs.sumOf { it.totalBytes }
    val bytesDone: Long get() = jobs.sumOf { it.bytesDone }
    val totalFiles: Int get() = jobs.sumOf { it.totalFiles }
    val filesDone: Int get() = jobs.sumOf { it.filesDone }
    val bytesPerSecond: Long get() = jobs.sumOf { it.bytesPerSecond }
    val fraction: Float get() = if (totalBytes > 0L) (bytesDone.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}
