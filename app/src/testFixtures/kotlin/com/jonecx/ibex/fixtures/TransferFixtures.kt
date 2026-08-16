package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.data.transfer.TransferProgress
import com.jonecx.ibex.data.transfer.TransferSnapshot
import com.jonecx.ibex.data.transfer.TransferStatus

private const val MB = 1024L * 1024L
private const val GB = 1024L * MB

// A single running job to an SMB share, roughly the mockup state, for the collapsed bar and the sheet.
fun runningTransferSnapshot(
    id: String = "job-1",
    operation: ClipboardOperation = ClipboardOperation.MOVE,
    status: TransferStatus = TransferStatus.RUNNING,
    totalFiles: Int = 5,
    filesDone: Int = 3,
    totalBytes: Long = 207 * MB,
    bytesDone: Long = 128 * MB,
    currentFileName: String = "IMG_2043.mp4",
    currentFileTotal: Long = 96 * MB,
    currentFileBytes: Long = (96 * MB * 41) / 100,
    bytesPerSecond: Long = 8_800_000L,
): TransferSnapshot = TransferSnapshot(
    listOf(
        runningTransferJob(
            id = id,
            operation = operation,
            status = status,
            totalFiles = totalFiles,
            filesDone = filesDone,
            totalBytes = totalBytes,
            bytesDone = bytesDone,
            currentFileName = currentFileName,
            currentFileTotal = currentFileTotal,
            currentFileBytes = currentFileBytes,
            bytesPerSecond = bytesPerSecond,
        ),
    ),
)

fun runningTransferJob(
    id: String = "job-1",
    operation: ClipboardOperation = ClipboardOperation.MOVE,
    status: TransferStatus = TransferStatus.RUNNING,
    destinationDir: String = "smb://NAS/Media/Camera",
    totalFiles: Int = 5,
    filesDone: Int = 3,
    totalBytes: Long = 207 * MB,
    bytesDone: Long = 128 * MB,
    currentFileName: String = "IMG_2043.mp4",
    currentFileTotal: Long = 96 * MB,
    currentFileBytes: Long = (96 * MB * 41) / 100,
    bytesPerSecond: Long = 8_800_000L,
    itemCount: Int = 5,
): TransferProgress = TransferProgress(
    id = id,
    operation = operation,
    destinationDir = destinationDir,
    status = status,
    totalBytes = totalBytes,
    totalFiles = totalFiles,
    bytesDone = bytesDone,
    filesDone = filesDone,
    currentFileName = currentFileName,
    bytesPerSecond = if (status == TransferStatus.RUNNING) bytesPerSecond else 0L,
    currentFileBytes = currentFileBytes,
    currentFileTotal = currentFileTotal,
    itemCount = itemCount,
)

// A queued second job (already measured), the "Copy to Internal ▸ Backup · 40 files · 1.2 GB" card.
fun queuedTransferJob(
    id: String = "job-2",
    operation: ClipboardOperation = ClipboardOperation.COPY,
    destinationDir: String = "/storage/emulated/0/Backup",
    totalFiles: Int = 40,
    totalBytes: Long = (12 * GB) / 10,
    itemCount: Int = 40,
): TransferProgress = TransferProgress(
    id = id,
    operation = operation,
    destinationDir = destinationDir,
    status = TransferStatus.QUEUED,
    totalBytes = totalBytes,
    totalFiles = totalFiles,
    bytesDone = 0L,
    filesDone = 0,
    itemCount = itemCount,
)

// The full sheet: an active SMB move plus a queued local copy behind it, matching the mockup.
fun sheetTransferSnapshot(
    running: TransferProgress = runningTransferJob(),
    queued: TransferProgress = queuedTransferJob(),
): TransferSnapshot = TransferSnapshot(listOf(running, queued))

// Same job, paused: the card shows Resume and a frozen bar, no speed/ETA.
fun pausedTransferSnapshot(): TransferSnapshot = TransferSnapshot(
    listOf(
        runningTransferJob(status = TransferStatus.PAUSED),
        queuedTransferJob(),
    ),
)
