package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.model.VideoFeed
import com.jonecx.ibex.data.preferences.LiveStreamsPreferencesContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeLiveStreamsPreferences : LiveStreamsPreferencesContract {
    private val _streams = MutableStateFlow<List<VideoFeed>>(emptyList())
    override val streams: Flow<List<VideoFeed>> = _streams

    override suspend fun addStream(stream: VideoFeed) {
        _streams.update { it + stream }
    }

    override suspend fun removeStream(id: String) {
        _streams.update { list -> list.filter { it.id != id } }
    }

    override suspend fun updateStream(stream: VideoFeed) {
        _streams.update { list ->
            list.map { if (it.id == stream.id) stream else it }
        }
    }

    var seedCalls: Int = 0
        private set

    override suspend fun seedIfNeeded() {
        seedCalls++
    }

    fun currentStreams(): List<VideoFeed> = _streams.value

    fun reset() {
        _streams.value = emptyList()
    }
}
