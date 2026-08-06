package com.jonecx.ibex.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MediaViewerUiState(
            viewableFiles = mediaViewerArgs.viewableFiles,
            initialIndex = mediaViewerArgs.initialIndex,
        ),
    )
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()

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
        mediaViewerArgs.clear()
    }
}
