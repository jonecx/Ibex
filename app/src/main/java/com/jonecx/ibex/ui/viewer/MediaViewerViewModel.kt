package com.jonecx.ibex.ui.viewer

import androidx.lifecycle.ViewModel
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.ui.player.PlayerFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class MediaViewerUiState(
    val viewableFiles: List<FileItem> = emptyList(),
    val initialIndex: Int = 0,
)

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val mediaViewerArgs: MediaViewerArgs,
    val playerFactory: PlayerFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MediaViewerUiState(
            viewableFiles = mediaViewerArgs.viewableFiles,
            initialIndex = mediaViewerArgs.initialIndex,
        ),
    )
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        mediaViewerArgs.clear()
    }
}
