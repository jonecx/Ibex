package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ClipboardState(
    val files: List<FileItem> = emptyList(),
    val operation: ClipboardOperation? = null,
) {
    val hasContent: Boolean get() = files.isNotEmpty() && operation != null
}

// Holds the pending copy/move selection only. Running the transfer is the TransferManager's job:
// paste enqueues a durable job and returns immediately, so the clipboard never does I/O.
interface FileClipboardManager {
    val state: StateFlow<ClipboardState>
    fun setClipboard(files: List<FileItem>, operation: ClipboardOperation)
    fun clear()
}

open class DefaultFileClipboardManager : FileClipboardManager {

    private val _state = MutableStateFlow(ClipboardState())
    override val state: StateFlow<ClipboardState> = _state.asStateFlow()

    override fun setClipboard(files: List<FileItem>, operation: ClipboardOperation) {
        _state.update { ClipboardState(files = files, operation = operation) }
    }

    override fun clear() {
        _state.update { ClipboardState() }
    }
}
