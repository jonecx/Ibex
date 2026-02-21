package com.jonecx.ibex.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsPreferencesContract {
    private val dataStore = context.dataStore

    override val sendAnalyticsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SEND_ANALYTICS_ENABLED] ?: false
    }

    override suspend fun setSendAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SEND_ANALYTICS_ENABLED] = enabled
        }
    }

    companion object {
        private val SEND_ANALYTICS_ENABLED = booleanPreferencesKey("send_analytics_enabled")
    }
}
