package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.data.transfer.TransferManager
import com.jonecx.ibex.data.transfer.TransferSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class FakeTransferManager : TransferManager {

    data class Enqueued(
        val files: List<FileItem>,
        val operation: ClipboardOperation,
        val destinationDir: String,
    )

    val enqueued = mutableListOf<Enqueued>()
    val cancelledIds = mutableListOf<String>()
    val pausedIds = mutableListOf<String>()
    val resumedIds = mutableListOf<String>()
    var cancelAllCount = 0
    var pauseAllCount = 0
    var recoverCount = 0

    private val _snapshot = MutableStateFlow(TransferSnapshot())
    override val snapshot: StateFlow<TransferSnapshot> = _snapshot

    private val _completions = MutableSharedFlow<String>(extraBufferCapacity = 8)
    override val completions: SharedFlow<String> = _completions

    override fun enqueue(files: List<FileItem>, operation: ClipboardOperation, destinationDir: String) {
        enqueued.add(Enqueued(files, operation, destinationDir))
    }

    override fun cancel(jobId: String) {
        cancelledIds.add(jobId)
    }

    override fun cancelAll() {
        cancelAllCount += 1
    }

    override fun pause(jobId: String) {
        pausedIds.add(jobId)
    }

    override fun resume(jobId: String) {
        resumedIds.add(jobId)
    }

    override fun pauseAll() {
        pauseAllCount += 1
    }

    override fun recoverAndResume() {
        recoverCount += 1
    }

    override fun hasRemoteWork(): Boolean = false

    override suspend fun hasPendingWork(): Boolean = _snapshot.value.hasActive

    override suspend fun runQueue() = Unit

    fun setSnapshot(snapshot: TransferSnapshot) {
        _snapshot.value = snapshot
    }

    suspend fun emitCompletion(destinationDir: String) {
        _completions.emit(destinationDir)
    }
}
