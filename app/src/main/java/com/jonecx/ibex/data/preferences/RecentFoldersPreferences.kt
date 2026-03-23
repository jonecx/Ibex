package com.jonecx.ibex.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jonecx.ibex.data.model.RecentFolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.recentFoldersDataStore: DataStore<Preferences> by preferencesDataStore(
    name = STORE_NAME,
)

private const val STORE_NAME = "recent_folders"

@Singleton
class RecentFoldersPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecentFoldersPreferencesContract {

    private val dataStore = context.recentFoldersDataStore
    private val json = Json { ignoreUnknownKeys = true }

    override val recentFolders: Flow<List<RecentFolder>> = dataStore.data.map { preferences ->
        val raw = preferences[RECENT_FOLDERS_KEY] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<RecentFolder>>(raw) }.getOrDefault(emptyList())
    }

    override suspend fun addRecentFolder(folder: RecentFolder) {
        dataStore.edit { preferences ->
            val current = preferences[RECENT_FOLDERS_KEY]
                ?.let { runCatching { json.decodeFromString<List<RecentFolder>>(it) }.getOrDefault(emptyList()) }
                ?: emptyList()

            val updated = RecentFoldersPreferencesContract.buildUpdatedList(folder, current)

            preferences[RECENT_FOLDERS_KEY] = json.encodeToString(updated)
        }
    }

    override suspend fun clearRecentFolders() {
        dataStore.edit { preferences ->
            preferences.remove(RECENT_FOLDERS_KEY)
        }
    }

    companion object {
        private val RECENT_FOLDERS_KEY = stringPreferencesKey("recent_folders_json")
    }
}
