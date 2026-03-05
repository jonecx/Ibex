package com.jonecx.ibex.data.preferences

import com.jonecx.ibex.data.model.ViewMode
import kotlinx.coroutines.flow.Flow

interface SettingsPreferencesContract {
    val sendAnalyticsEnabled: Flow<Boolean>
    suspend fun setSendAnalyticsEnabled(enabled: Boolean)

    val viewMode: Flow<ViewMode>
    suspend fun setViewMode(mode: ViewMode)

    val gridColumns: Flow<Int>
    suspend fun setGridColumns(columns: Int)

    companion object {
        const val DEFAULT_GRID_COLUMNS = 4
        const val MIN_GRID_COLUMNS = 2
        const val MAX_GRID_COLUMNS = 6
        val GRID_COLUMN_OPTIONS = (MIN_GRID_COLUMNS..MAX_GRID_COLUMNS).toList()
    }
}
