package com.jonecx.ibex.ui.live

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.data.model.VideoFeed
import com.jonecx.ibex.data.preferences.LiveStreamsPreferencesContract
import com.jonecx.ibex.util.launchCollect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class LiveFeedUiState(
    val streams: List<VideoFeed> = emptyList(),
    val streamToEdit: VideoFeed? = null,
)

class LiveFeedViewModel(
    private val liveStreamsPreferences: LiveStreamsPreferencesContract,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveFeedUiState())
    val uiState: StateFlow<LiveFeedUiState> = _uiState.asStateFlow()

    init {
        // Seed the built-in Azmaree streams once so the grid isn't empty out of the box.
        viewModelScope.launch(dispatcher) {
            liveStreamsPreferences.seedIfNeeded()
        }
        viewModelScope.launchCollect(liveStreamsPreferences.streams, dispatcher) { streams ->
            _uiState.update { it.copy(streams = streams) }
        }
    }

    fun addStream(stream: VideoFeed) {
        viewModelScope.launch(dispatcher) {
            liveStreamsPreferences.addStream(stream)
        }
    }

    fun updateStream(stream: VideoFeed) {
        viewModelScope.launch(dispatcher) {
            liveStreamsPreferences.updateStream(stream)
        }
    }

    fun removeStream(id: String) {
        viewModelScope.launch(dispatcher) {
            liveStreamsPreferences.removeStream(id)
        }
    }

    fun setStreamToEdit(stream: VideoFeed) {
        _uiState.update { it.copy(streamToEdit = stream) }
    }

    fun clearStreamToEdit() {
        _uiState.update { it.copy(streamToEdit = null) }
    }
}
