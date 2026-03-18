package com.jonecx.ibex.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract.Companion.DEFAULT_GRID_COLUMNS
import com.jonecx.ibex.di.IoDispatcher
import com.jonecx.ibex.util.launchCollect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val sendAnalyticsEnabled: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    val gridColumns: Int = DEFAULT_GRID_COLUMNS,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferencesContract,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
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

    fun setSendAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch(dispatcher) {
            settingsPreferences.setSendAnalyticsEnabled(enabled)
        }
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch(dispatcher) {
            settingsPreferences.setViewMode(mode)
        }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch(dispatcher) {
            settingsPreferences.setGridColumns(columns)
        }
    }
}
