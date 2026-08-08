package com.jonecx.ibex.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.data.model.ThemeMode
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract.Companion.DEFAULT_GRID_COLUMNS
import com.jonecx.ibex.util.launchCollect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sendAnalyticsEnabled: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    val gridColumns: Int = DEFAULT_GRID_COLUMNS,
)

class SettingsViewModel(
    private val settingsPreferences: SettingsPreferencesContract,
    private val analyticsManager: AnalyticsManager,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launchCollect(settingsPreferences.themeMode, dispatcher) { mode ->
            _uiState.update { it.copy(themeMode = mode) }
        }
        viewModelScope.launchCollect(settingsPreferences.sendAnalyticsEnabled, dispatcher) { enabled ->
            _uiState.update { it.copy(sendAnalyticsEnabled = enabled) }
        }
        viewModelScope.launchCollect(settingsPreferences.viewMode, dispatcher) { mode ->
            _uiState.update { it.copy(viewMode = mode) }
        }
        viewModelScope.launchCollect(settingsPreferences.gridColumns, dispatcher) { columns ->
            _uiState.update { it.copy(gridColumns = columns) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        val from = _uiState.value.themeMode
        if (from != mode) analyticsManager.trackThemeChange(from = from, to = mode)
        viewModelScope.launch(dispatcher) {
            settingsPreferences.setThemeMode(mode)
        }
    }

    fun setSendAnalyticsEnabled(enabled: Boolean) {
        // Disabling revokes consent, after which capture() no-ops; emit the change first so the
        // opt-out itself is recorded. Enabling is emitted after the write.
        if (!enabled) analyticsManager.trackAnalyticsConsentChange(granted = false)
        viewModelScope.launch(dispatcher) {
            settingsPreferences.setSendAnalyticsEnabled(enabled)
        }
        if (enabled) analyticsManager.trackAnalyticsConsentChange(granted = true)
    }

    fun setViewMode(mode: ViewMode) {
        val from = _uiState.value.viewMode
        if (from != mode) analyticsManager.trackViewModeChange(from = from, to = mode)
        viewModelScope.launch(dispatcher) {
            settingsPreferences.setViewMode(mode)
        }
    }

    fun setGridColumns(columns: Int) {
        val from = _uiState.value.gridColumns
        if (from != columns) analyticsManager.trackGridColumnsChange(from = from, to = columns)
        viewModelScope.launch(dispatcher) {
            settingsPreferences.setGridColumns(columns)
        }
    }
}
