package com.jonecx.ibex.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jonecx.ibex.data.model.VideoFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.liveStreamsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = LiveStreamsPreferences.STORE_NAME,
)

class LiveStreamsPreferences(
    context: Context,
) : LiveStreamsPreferencesContract {
    private val dataStore = context.liveStreamsDataStore

    override val streams: Flow<List<VideoFeed>> = dataStore.data.map { it.currentStreams() }

    override suspend fun addStream(stream: VideoFeed) {
        dataStore.edit { preferences ->
            preferences[STREAMS_KEY] = toJson(preferences.currentStreams() + stream)
        }
    }

    override suspend fun removeStream(id: String) {
        dataStore.edit { preferences ->
            preferences[STREAMS_KEY] = toJson(preferences.currentStreams().filter { it.id != id })
        }
    }

    override suspend fun updateStream(stream: VideoFeed) {
        dataStore.edit { preferences ->
            val updated = preferences.currentStreams().map { if (it.id == stream.id) stream else it }
            preferences[STREAMS_KEY] = toJson(updated)
        }
    }

    private fun Preferences.currentStreams(): List<VideoFeed> =
        Json.decodeFromString(this[STREAMS_KEY] ?: EMPTY_JSON_ARRAY)

    private fun toJson(streams: List<VideoFeed>): String = Json.encodeToString(streams)

    companion object {
        const val STORE_NAME = "live_streams"
        private val STREAMS_KEY = stringPreferencesKey(STORE_NAME)
        private const val EMPTY_JSON_ARRAY = "[]"
    }
}
