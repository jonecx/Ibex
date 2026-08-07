package com.jonecx.ibex.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.jonecx.azmaree.player.model.PlayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Separate store from "settings" so player tuning and app preferences evolve independently.
private val Context.playerSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "player_settings")

class PlayerSettingsPreferences(
    private val context: Context,
) : PlayerSettingsPreferencesContract {

    override val settings: Flow<PlayerSettings> =
        context.playerSettingsStore.data.map { it.toPlayerSettings() }

    override suspend fun update(transform: (PlayerSettings) -> PlayerSettings) {
        context.playerSettingsStore.edit { preferences ->
            preferences.writeFrom(transform(preferences.toPlayerSettings()))
        }
    }

    override suspend fun resetToDefaults() {
        context.playerSettingsStore.edit { it.clear() }
    }
}
