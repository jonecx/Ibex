package com.jonecx.ibex.data.preferences

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jonecx.azmaree.player.model.CastConfig
import com.jonecx.azmaree.player.model.PlayButtonPosition
import com.jonecx.azmaree.player.model.PlayerSettings
import com.jonecx.azmaree.player.model.PlayerStyle
import kotlinx.coroutines.flow.Flow

// Persisted overrides for the Azmaree player. The UI edits the domain PlayerSettings directly and
// hands back a transform; storage keys never leave this data-layer file, so the ViewModel and
// composables stay free of DataStore types.
interface PlayerSettingsPreferencesContract {
    val settings: Flow<PlayerSettings>

    suspend fun update(transform: (PlayerSettings) -> PlayerSettings)

    suspend fun resetToDefaults()

    companion object {
        // Ibex ships cast visible; everything else is the SDK default.
        val DEFAULTS: PlayerSettings = PlayerSettings(cast = CastConfig(enabled = true))

        // Accessibility preset the "large controls" toggle switches the style to.
        val LARGE_CONTROLS_STYLE: PlayerStyle = PlayerStyle(
            controlIconSize = 48.dp,
            playPauseButtonSize = 96.dp,
            playPauseIconSize = 72.dp,
            timeLabelSize = 16.sp,
        )
    }
}

// Stored by stable name so a dropped field falls back to the SDK default instead of crashing.
internal object PlayerSettingsKeys {
    val PLAY_BUTTON_POSITION = stringPreferencesKey("play_button_position")
    val SEEK_STEP_MS = longPreferencesKey("seek_step_ms")
    val SUBTITLES_DEFAULT = booleanPreferencesKey("subtitles_default")
    val VOLUME_SWIPE = booleanPreferencesKey("volume_swipe")
    val BRIGHTNESS_SWIPE = booleanPreferencesKey("brightness_swipe")
    val MAX_VIDEO_HEIGHT = intPreferencesKey("max_video_height")
    val LARGE_CONTROLS = booleanPreferencesKey("large_controls")
    val CAST_ENABLED = booleanPreferencesKey("cast_enabled")
    val MIN_BUFFER_MS = intPreferencesKey("min_buffer_ms")
    val MAX_BUFFER_MS = intPreferencesKey("max_buffer_ms")
    val BUFFER_FOR_PLAYBACK_MS = intPreferencesKey("buffer_for_playback_ms")
    val BUFFER_AFTER_REBUFFER_MS = intPreferencesKey("buffer_after_rebuffer_ms")
    val MAX_BITRATE = intPreferencesKey("max_bitrate")
    val MAX_VIDEO_WIDTH = intPreferencesKey("max_video_width")
    val MAX_POOL_SIZE = intPreferencesKey("max_pool_size")
    val PROGRESS_REFRESH_MS = longPreferencesKey("progress_refresh_ms")
    val HD_MIN_HEIGHT = intPreferencesKey("hd_min_height")
    val UHD_MIN_HEIGHT = intPreferencesKey("uhd_min_height")
    val FALLBACK_BRIGHTNESS = floatPreferencesKey("fallback_brightness")
    val DEBUG_OVERLAY = booleanPreferencesKey("debug_overlay")
}

// Anything unset falls back to the SDK default, so this stays correct if defaults change.
internal fun Preferences.toPlayerSettings(): PlayerSettings {
    val keys = PlayerSettingsKeys
    val defaults = PlayerSettings()

    // Clamp so no stored combination violates DefaultLoadControl's buffer ordering rules.
    val bufferForPlaybackMs = this[keys.BUFFER_FOR_PLAYBACK_MS]
        ?: defaults.playback.buffer.bufferForPlaybackMs
    val bufferAfterRebufferMs = this[keys.BUFFER_AFTER_REBUFFER_MS]
        ?: defaults.playback.buffer.bufferForPlaybackAfterRebufferMs
    val minBufferMs = (this[keys.MIN_BUFFER_MS] ?: defaults.playback.buffer.minBufferMs)
        .coerceAtLeast(maxOf(bufferForPlaybackMs, bufferAfterRebufferMs))
    val maxBufferMs = (this[keys.MAX_BUFFER_MS] ?: defaults.playback.buffer.maxBufferMs)
        .coerceAtLeast(minBufferMs)

    val style = if (this[keys.LARGE_CONTROLS] == true) {
        PlayerSettingsPreferencesContract.LARGE_CONTROLS_STYLE
    } else {
        defaults.style
    }

    return PlayerSettings(
        playback = defaults.playback.copy(
            maxVideoHeight = this[keys.MAX_VIDEO_HEIGHT] ?: defaults.playback.maxVideoHeight,
            maxVideoWidth = this[keys.MAX_VIDEO_WIDTH] ?: defaults.playback.maxVideoWidth,
            maxBitrate = this[keys.MAX_BITRATE] ?: defaults.playback.maxBitrate,
            maxPoolSize = this[keys.MAX_POOL_SIZE] ?: defaults.playback.maxPoolSize,
            buffer = defaults.playback.buffer.copy(
                minBufferMs = minBufferMs,
                maxBufferMs = maxBufferMs,
                bufferForPlaybackMs = bufferForPlaybackMs,
                bufferForPlaybackAfterRebufferMs = bufferAfterRebufferMs,
            ),
        ),
        controls = defaults.controls.copy(
            seekStepMs = this[keys.SEEK_STEP_MS] ?: defaults.controls.seekStepMs,
            progressRefreshIntervalMs = this[keys.PROGRESS_REFRESH_MS]
                ?: defaults.controls.progressRefreshIntervalMs,
            subtitlesEnabledByDefault = this[keys.SUBTITLES_DEFAULT]
                ?: defaults.controls.subtitlesEnabledByDefault,
            hdMinHeight = this[keys.HD_MIN_HEIGHT] ?: defaults.controls.hdMinHeight,
            uhdMinHeight = this[keys.UHD_MIN_HEIGHT] ?: defaults.controls.uhdMinHeight,
            playButtonPosition = playButtonPosition(defaults.controls.playButtonPosition),
        ),
        gestures = defaults.gestures.copy(
            volumeSwipeEnabled = this[keys.VOLUME_SWIPE] ?: defaults.gestures.volumeSwipeEnabled,
            brightnessSwipeEnabled = this[keys.BRIGHTNESS_SWIPE]
                ?: defaults.gestures.brightnessSwipeEnabled,
            fallbackBrightness = this[keys.FALLBACK_BRIGHTNESS]
                ?: defaults.gestures.fallbackBrightness,
        ),
        style = style,
        // On unless the user opted out: the SDK ships cast disabled, the app ships it visible.
        cast = defaults.cast.copy(enabled = this[keys.CAST_ENABLED] ?: true),
        // Off by default: the stats overlay is a developer aid, not an end-user surface.
        debug = defaults.debug.copy(statsOverlayEnabled = this[keys.DEBUG_OVERLAY] ?: false),
    )
}

// Inverse of toPlayerSettings: flatten the edited domain object back onto the stored keys.
internal fun MutablePreferences.writeFrom(settings: PlayerSettings) {
    val keys = PlayerSettingsKeys
    this[keys.PLAY_BUTTON_POSITION] = settings.controls.playButtonPosition.name
    this[keys.SEEK_STEP_MS] = settings.controls.seekStepMs
    this[keys.SUBTITLES_DEFAULT] = settings.controls.subtitlesEnabledByDefault
    this[keys.VOLUME_SWIPE] = settings.gestures.volumeSwipeEnabled
    this[keys.BRIGHTNESS_SWIPE] = settings.gestures.brightnessSwipeEnabled
    this[keys.MAX_VIDEO_HEIGHT] = settings.playback.maxVideoHeight
    this[keys.LARGE_CONTROLS] = settings.style != PlayerStyle()
    this[keys.CAST_ENABLED] = settings.cast.enabled
    this[keys.MIN_BUFFER_MS] = settings.playback.buffer.minBufferMs
    this[keys.MAX_BUFFER_MS] = settings.playback.buffer.maxBufferMs
    this[keys.BUFFER_FOR_PLAYBACK_MS] = settings.playback.buffer.bufferForPlaybackMs
    this[keys.BUFFER_AFTER_REBUFFER_MS] = settings.playback.buffer.bufferForPlaybackAfterRebufferMs
    this[keys.MAX_BITRATE] = settings.playback.maxBitrate
    this[keys.MAX_VIDEO_WIDTH] = settings.playback.maxVideoWidth
    this[keys.MAX_POOL_SIZE] = settings.playback.maxPoolSize
    this[keys.PROGRESS_REFRESH_MS] = settings.controls.progressRefreshIntervalMs
    this[keys.HD_MIN_HEIGHT] = settings.controls.hdMinHeight
    this[keys.UHD_MIN_HEIGHT] = settings.controls.uhdMinHeight
    this[keys.FALLBACK_BRIGHTNESS] = settings.gestures.fallbackBrightness
    this[keys.DEBUG_OVERLAY] = settings.debug.statsOverlayEnabled
}

// Stored by name, so a value dropped from the enum falls back instead of crashing.
private fun Preferences.playButtonPosition(default: PlayButtonPosition): PlayButtonPosition =
    PlayButtonPosition.entries.firstOrNull {
        it.name == this[PlayerSettingsKeys.PLAY_BUTTON_POSITION]
    } ?: default
