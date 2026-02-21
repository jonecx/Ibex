package com.jonecx.ibex.data.preferences

import kotlinx.coroutines.flow.Flow

interface SettingsPreferencesContract {
    val sendAnalyticsEnabled: Flow<Boolean>
    suspend fun setSendAnalyticsEnabled(enabled: Boolean)
}
