package com.jonecx.ibex.data.preferences

import com.jonecx.ibex.data.model.ViewMode
import kotlinx.coroutines.flow.Flow

interface SettingsPreferencesContract {
    val sendAnalyticsEnabled: Flow<Boolean>
    suspend fun setSendAnalyticsEnabled(enabled: Boolean)

    val viewMode: Flow<ViewMode>
    suspend fun setViewMode(mode: ViewMode)
}
