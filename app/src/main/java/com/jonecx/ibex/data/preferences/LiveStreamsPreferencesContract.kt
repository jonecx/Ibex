package com.jonecx.ibex.data.preferences

import com.jonecx.ibex.data.model.VideoFeed
import kotlinx.coroutines.flow.Flow

interface LiveStreamsPreferencesContract {
    val streams: Flow<List<VideoFeed>>
    suspend fun addStream(stream: VideoFeed)
    suspend fun removeStream(id: String)
    suspend fun updateStream(stream: VideoFeed)
}
