package com.jonecx.ibex.ui.viewer

import androidx.lifecycle.ViewModel
import com.jonecx.ibex.data.model.FileItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ImageViewerUiState(
    val viewableFiles: List<FileItem> = emptyList(),
    val initialIndex: Int = 0,
)

@HiltViewModel
class ImageViewerViewModel @Inject constructor(
    private val imageViewerArgs: ImageViewerArgs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ImageViewerUiState(
            viewableFiles = imageViewerArgs.viewableFiles,
            initialIndex = imageViewerArgs.initialIndex,
        ),
    )
    val uiState: StateFlow<ImageViewerUiState> = _uiState.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        imageViewerArgs.clear()
    }
}
