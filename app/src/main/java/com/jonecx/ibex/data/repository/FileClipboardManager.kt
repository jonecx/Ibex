package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class ClipboardState(
    val files: List<FileItem> = emptyList(),
    val operation: ClipboardOperation? = null,
) {
    val hasContent: Boolean get() = files.isNotEmpty() && operation != null
}

interface FileClipboardManager {
    val state: StateFlow<ClipboardState>
    fun setClipboard(files: List<FileItem>, operation: ClipboardOperation)
    fun clear()
    suspend fun paste(destinationDir: String): Boolean
}

@Singleton
open class DefaultFileClipboardManager @Inject constructor(
    private val fileMoveManager: FileMoveManager,
) : FileClipboardManager {

    private val _state = MutableStateFlow(ClipboardState())
    override val state: StateFlow<ClipboardState> = _state.asStateFlow()

    override fun setClipboard(files: List<FileItem>, operation: ClipboardOperation) {
        _state.update { ClipboardState(files = files, operation = operation) }
    }

    override fun clear() {
        _state.update { ClipboardState() }
    }

    override suspend fun paste(destinationDir: String): Boolean = coroutineScope {
        val clipboard = _state.value
        if (!clipboard.hasContent) return@coroutineScope false

        val results = clipboard.files.map { file ->
            async {
                when (clipboard.operation) {
                    ClipboardOperation.MOVE -> fileMoveManager.moveFile(file, destinationDir)
                    ClipboardOperation.COPY -> fileMoveManager.copyFile(file, destinationDir)
                    null -> false
                }
            }
        }.awaitAll()

        clear()
        results.all { it }
    }
}
