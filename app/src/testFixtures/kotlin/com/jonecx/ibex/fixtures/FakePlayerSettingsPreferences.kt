package com.jonecx.ibex.fixtures

import com.jonecx.azmaree.player.model.PlayerSettings
import com.jonecx.ibex.data.preferences.PlayerSettingsPreferencesContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// In-memory PlayerSettings, applying the same transforms the real store persists.
class FakePlayerSettingsPreferences : PlayerSettingsPreferencesContract {

    private val _settings = MutableStateFlow(PlayerSettingsPreferencesContract.DEFAULTS)
    override val settings: Flow<PlayerSettings> = _settings.asStateFlow()

    override suspend fun update(transform: (PlayerSettings) -> PlayerSettings) {
        _settings.update(transform)
    }

    override suspend fun resetToDefaults() {
        _settings.value = PlayerSettingsPreferencesContract.DEFAULTS
    }

    fun currentSettings(): PlayerSettings = _settings.value

    fun reset() {
        _settings.value = PlayerSettingsPreferencesContract.DEFAULTS
    }
}
