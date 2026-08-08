package com.jonecx.ibex.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.repository.FileTrashManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MediaViewerUiState(
    val viewableFiles: List<FileItem> = emptyList(),
    val initialIndex: Int = 0,
)

class MediaViewerViewModel(
    private val mediaViewerArgs: MediaViewerArgs,
    private val fileTrashManager: FileTrashManager,
    private val analyticsManager: AnalyticsManager,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MediaViewerUiState(
            viewableFiles = mediaViewerArgs.viewableFiles,
            initialIndex = mediaViewerArgs.initialIndex,
        ),
    )
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()

    // Viewer-session telemetry (video playback QoE is handled separately by PlayerTelemetry).
    private val openedAtMs = System.currentTimeMillis()
    private val visitedPages = mutableSetOf<Int>()
    private var lastPage = mediaViewerArgs.initialIndex

    init {
        val files = mediaViewerArgs.viewableFiles
        if (files.isNotEmpty()) {
            val index = mediaViewerArgs.initialIndex.coerceIn(0, files.lastIndex)
            visitedPages.add(index)
            val file = files[index]
            analyticsManager.trackMediaViewerOpen(
                itemCount = files.size,
                mediaType = file.fileType,
                isRemote = file.isRemote,
                initialIndex = index,
            )
        }
    }

    // Called when the pager settles on a page; the initial settle is skipped (index == lastPage).
    fun onPageChanged(index: Int) {
        if (index == lastPage) return
        val file = _uiState.value.viewableFiles.getOrNull(index) ?: return
        val forward = index > lastPage
        visitedPages.add(index)
        analyticsManager.trackMediaViewerPage(mediaType = file.fileType, forward = forward, pageIndex = index)
        lastPage = index
    }

    fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch(ioDispatcher) {
            val trashed = fileTrashManager.trashFile(fileItem)
            if (trashed) {
                _uiState.update { state ->
                    val updatedFiles = state.viewableFiles.filterNot { it.path == fileItem.path }
                    state.copy(viewableFiles = updatedFiles)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (visitedPages.isNotEmpty()) {
            analyticsManager.trackMediaViewerClose(
                durationMs = System.currentTimeMillis() - openedAtMs,
                pagesViewed = visitedPages.size,
            )
        }
        mediaViewerArgs.clear()
    }
}
