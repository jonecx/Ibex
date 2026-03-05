package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract.Companion.DEFAULT_GRID_COLUMNS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsPreferences : SettingsPreferencesContract {
    private val _sendAnalyticsEnabled = MutableStateFlow(false)
    override val sendAnalyticsEnabled: Flow<Boolean> = _sendAnalyticsEnabled

    override suspend fun setSendAnalyticsEnabled(enabled: Boolean) {
        _sendAnalyticsEnabled.value = enabled
    }

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    override val viewMode: Flow<ViewMode> = _viewMode

    override suspend fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    private val _gridColumns = MutableStateFlow(DEFAULT_GRID_COLUMNS)
    override val gridColumns: Flow<Int> = _gridColumns

    override suspend fun setGridColumns(columns: Int) {
        _gridColumns.value = columns
    }

    fun currentAnalyticsValue(): Boolean = _sendAnalyticsEnabled.value
    fun currentViewMode(): ViewMode = _viewMode.value
    fun currentGridColumns(): Int = _gridColumns.value

    fun reset() {
        _sendAnalyticsEnabled.value = false
        _viewMode.value = ViewMode.LIST
        _gridColumns.value = DEFAULT_GRID_COLUMNS
    }
}
