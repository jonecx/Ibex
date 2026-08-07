package com.jonecx.ibex.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.azmaree.player.model.PlayerSettings
import com.jonecx.ibex.data.preferences.PlayerSettingsPreferencesContract
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerSettingsViewModel(
    private val preferences: PlayerSettingsPreferencesContract,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    // Null until the first DataStore emission so the UI shows a spinner instead of flashing defaults.
    val settings: StateFlow<PlayerSettings?> = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (PlayerSettings) -> PlayerSettings) {
        viewModelScope.launch(dispatcher) { preferences.update(transform) }
    }

    fun resetToDefaults() {
        viewModelScope.launch(dispatcher) { preferences.resetToDefaults() }
    }
}
