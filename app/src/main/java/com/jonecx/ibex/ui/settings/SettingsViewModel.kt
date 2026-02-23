package com.jonecx.ibex.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val sendAnalyticsEnabled: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferencesContract,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            settingsPreferences.sendAnalyticsEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(sendAnalyticsEnabled = enabled)
            }
        }
        viewModelScope.launch(dispatcher) {
            settingsPreferences.viewMode.collect { mode ->
                _uiState.value = _uiState.value.copy(viewMode = mode)
            }
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
}
