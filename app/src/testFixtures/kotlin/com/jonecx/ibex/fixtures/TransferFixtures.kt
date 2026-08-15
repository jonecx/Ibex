package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.data.transfer.TransferProgress
import com.jonecx.ibex.data.transfer.TransferSnapshot
import com.jonecx.ibex.data.transfer.TransferStatus

// A single running job, roughly the mockup state, for previews and UI tests.
fun runningTransferSnapshot(
    id: String = "job-1",
    operation: ClipboardOperation = ClipboardOperation.MOVE,
    totalFiles: Int = 3,
    filesDone: Int = 1,
    totalBytes: Long = 207L * 1024 * 1024,
    bytesDone: Long = 128L * 1024 * 1024,
    currentFileName: String = "IMG_2043.mp4",
    bytesPerSecond: Long = 8_400_000L,
): TransferSnapshot = TransferSnapshot(
    listOf(
        TransferProgress(
            id = id,
            operation = operation,
            destinationDir = "smb://NAS/Media/Camera",
            status = TransferStatus.RUNNING,
            totalBytes = totalBytes,
            totalFiles = totalFiles,
            bytesDone = bytesDone,
            filesDone = filesDone,
            currentFileName = currentFileName,
            bytesPerSecond = bytesPerSecond,
        ),
    ),
)
