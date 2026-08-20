package com.jonecx.ibex.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jonecx.azmaree.player.model.PlaybackConfig
import com.jonecx.azmaree.player.model.PlayerSettings
import com.jonecx.azmaree.player.model.PlayerStyle
import com.jonecx.ibex.R
import com.jonecx.ibex.data.preferences.PlayerSettingsPreferencesContract
import com.jonecx.ibex.ui.components.IbexTopAppBar
import com.jonecx.ibex.ui.settings.components.SettingsChoiceRow
import com.jonecx.ibex.ui.settings.components.SettingsSectionHeader
import com.jonecx.ibex.ui.settings.components.SettingsSliderRow
import com.jonecx.ibex.ui.settings.components.SettingsSwitchRow
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import kotlin.math.roundToInt

// A transform applied to the current settings; the only event this screen emits upward.
private typealias SettingsEdit = ((PlayerSettings) -> PlayerSettings) -> Unit

// "Auto"/"Unlimited" caps are stored as the SDK's no-limit value.
private const val NO_CAP = Int.MAX_VALUE

@Composable
fun PlayerSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerSettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    PlayerSettingsScreenContent(
        settings = settings,
        onNavigateBack = onNavigateBack,
        onUpdate = viewModel::update,
        onReset = viewModel::resetToDefaults,
        modifier = modifier,
    )
}

/**
 * Preference UI for everything PlayerSettings exposes, wired into the Azmaree player Ibex embeds.
 * PlayerStyle colors are deliberately absent, they follow the app theme and its brand accent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerSettingsScreenContent(
    settings: PlayerSettings?,
    onNavigateBack: () -> Unit,
    onUpdate: SettingsEdit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            IbexTopAppBar(
                title = stringResource(R.string.settings_player),
                onNavigateBack = onNavigateBack,
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        if (settings == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            PlaybackSection(settings, onUpdate)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AdvancedSection(settings, onUpdate)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DeveloperSection(settings, onUpdate)
            TextButton(
                onClick = onReset,
                modifier = Modifier.padding(vertical = 16.dp),
            ) {
                Text(stringResource(R.string.settings_reset))
            }
        }
    }
}

@Composable
private fun PlaybackSection(settings: PlayerSettings, onUpdate: SettingsEdit) {
    SettingsSectionHeader(stringResource(R.string.settings_section_simple))
    SettingsChoiceRow(
        title = stringResource(R.string.settings_seek_step),
        options = listOf(5_000L, 10_000L, 30_000L).map {
            stringResource(R.string.settings_value_seconds, (it / 1_000).toString()) to it
        },
        selected = settings.controls.seekStepMs,
        onSelect = { value -> onUpdate { it.copy(controls = it.controls.copy(seekStepMs = value)) } },
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_subtitles_default),
        checked = settings.controls.subtitlesEnabledByDefault,
        onChange = { value ->
            onUpdate { it.copy(controls = it.controls.copy(subtitlesEnabledByDefault = value)) }
        },
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_cast),
        hint = stringResource(R.string.settings_cast_hint),
        checked = settings.cast.enabled,
        onChange = { value -> onUpdate { it.copy(cast = it.cast.copy(enabled = value)) } },
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_volume_swipe),
        checked = settings.gestures.volumeSwipeEnabled,
        onChange = { value ->
            onUpdate { it.copy(gestures = it.gestures.copy(volumeSwipeEnabled = value)) }
        },
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_brightness_swipe),
        checked = settings.gestures.brightnessSwipeEnabled,
        onChange = { value ->
            onUpdate { it.copy(gestures = it.gestures.copy(brightnessSwipeEnabled = value)) }
        },
    )
    SettingsChoiceRow(
        title = stringResource(R.string.settings_quality_cap),
        hint = stringResource(R.string.settings_quality_cap_hint),
        options = listOf(stringResource(R.string.settings_option_auto) to NO_CAP) +
            listOf(480, 720, 1080).map { stringResource(R.string.settings_value_pixels, it) to it },
        selected = settings.playback.maxVideoHeight,
        onSelect = { value -> onUpdate { it.copy(playback = it.playback.copy(maxVideoHeight = value)) } },
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_large_controls),
        hint = stringResource(R.string.settings_large_controls_hint),
        checked = settings.style != PlayerStyle(),
        onChange = { value ->
            val style = if (value) PlayerSettingsPreferencesContract.LARGE_CONTROLS_STYLE else PlayerStyle()
            onUpdate { it.copy(style = style) }
        },
    )
}

@Composable
private fun AdvancedSection(settings: PlayerSettings, onUpdate: SettingsEdit) {
    val buffer = settings.playback.buffer

    SettingsSectionHeader(stringResource(R.string.settings_section_advanced))
    SettingsSliderRow(
        title = stringResource(R.string.settings_min_buffer),
        value = buffer.minBufferMs / 1_000f,
        range = 5f..60f,
        steps = 10,
        valueLabel = { WholeSecondsLabel(it) },
        onCommit = { seconds ->
            onUpdate {
                it.copy(playback = it.playback.copy(buffer = it.playback.buffer.copy(minBufferMs = seconds.roundToInt() * 1_000)))
            }
        },
    )
    SettingsSliderRow(
        title = stringResource(R.string.settings_max_buffer),
        value = buffer.maxBufferMs / 1_000f,
        range = 10f..120f,
        steps = 10,
        valueLabel = { WholeSecondsLabel(it) },
        onCommit = { seconds ->
            onUpdate {
                it.copy(playback = it.playback.copy(buffer = it.playback.buffer.copy(maxBufferMs = seconds.roundToInt() * 1_000)))
            }
        },
    )
    SettingsSliderRow(
        title = stringResource(R.string.settings_buffer_for_playback),
        value = buffer.bufferForPlaybackMs / 1_000f,
        range = 0.5f..10f,
        steps = 18,
        valueLabel = { stringResource(R.string.settings_value_seconds, String.format(Locale.getDefault(), "%.1f", it)) },
        onCommit = { seconds ->
            onUpdate {
                it.copy(playback = it.playback.copy(buffer = it.playback.buffer.copy(bufferForPlaybackMs = (seconds * 1_000).roundToInt())))
            }
        },
    )
    SettingsSliderRow(
        title = stringResource(R.string.settings_buffer_after_rebuffer),
        value = buffer.bufferForPlaybackAfterRebufferMs / 1_000f,
        range = 1f..15f,
        steps = 13,
        valueLabel = { WholeSecondsLabel(it) },
        onCommit = { seconds ->
            onUpdate {
                it.copy(playback = it.playback.copy(buffer = it.playback.buffer.copy(bufferForPlaybackAfterRebufferMs = seconds.roundToInt() * 1_000)))
            }
        },
    )
    SettingsChoiceRow(
        title = stringResource(R.string.settings_max_bitrate),
        options = listOf(stringResource(R.string.settings_option_unlimited) to NO_CAP) +
            listOf(2, 5, 10).map { stringResource(R.string.settings_value_mbps, it) to it * 1_000_000 },
        selected = settings.playback.maxBitrate,
        onSelect = { value -> onUpdate { it.copy(playback = it.playback.copy(maxBitrate = value)) } },
    )
    SettingsChoiceRow(
        title = stringResource(R.string.settings_max_video_width),
        options = listOf(stringResource(R.string.settings_option_unlimited) to NO_CAP) +
            listOf(1280, 1920, 3840).map { stringResource(R.string.settings_value_pixels, it) to it },
        selected = settings.playback.maxVideoWidth,
        onSelect = { value -> onUpdate { it.copy(playback = it.playback.copy(maxVideoWidth = value)) } },
    )
    SettingsChoiceRow(
        title = stringResource(R.string.settings_pool_size),
        hint = stringResource(R.string.settings_pool_size_hint),
        options = (PlaybackConfig.MIN_POOL_SIZE..PlaybackConfig.MAX_POOL_SIZE).map { it.toString() to it },
        selected = settings.playback.maxPoolSize,
        onSelect = { value -> onUpdate { it.copy(playback = it.playback.copy(maxPoolSize = value)) } },
    )
    SettingsChoiceRow(
        title = stringResource(R.string.settings_progress_refresh),
        options = listOf(250L, 500L, 1_000L).map {
            stringResource(R.string.settings_value_millis, it.toInt()) to it
        },
        selected = settings.controls.progressRefreshIntervalMs,
        onSelect = { value ->
            onUpdate { it.copy(controls = it.controls.copy(progressRefreshIntervalMs = value)) }
        },
    )
    SettingsChoiceRow(
        title = stringResource(R.string.settings_hd_threshold),
        options = listOf(720, 1080).map { stringResource(R.string.settings_value_pixels, it) to it },
        selected = settings.controls.hdMinHeight,
        onSelect = { value -> onUpdate { it.copy(controls = it.controls.copy(hdMinHeight = value)) } },
    )
    SettingsChoiceRow(
        title = stringResource(R.string.settings_uhd_threshold),
        options = listOf(1440, 2160).map { stringResource(R.string.settings_value_pixels, it) to it },
        selected = settings.controls.uhdMinHeight,
        onSelect = { value -> onUpdate { it.copy(controls = it.controls.copy(uhdMinHeight = value)) } },
    )
    SettingsSliderRow(
        title = stringResource(R.string.settings_fallback_brightness),
        hint = stringResource(R.string.settings_fallback_brightness_hint),
        value = settings.gestures.fallbackBrightness,
        range = 0f..1f,
        steps = 0,
        valueLabel = { stringResource(R.string.settings_value_percent, (it * 100).roundToInt()) },
        onCommit = { level -> onUpdate { it.copy(gestures = it.gestures.copy(fallbackBrightness = level)) } },
    )
}

@Composable
private fun DeveloperSection(settings: PlayerSettings, onUpdate: SettingsEdit) {
    SettingsSectionHeader(stringResource(R.string.settings_section_developer))
    SettingsSwitchRow(
        title = stringResource(R.string.settings_debug_overlay),
        hint = stringResource(R.string.settings_debug_overlay_hint),
        checked = settings.debug.statsOverlayEnabled,
        onChange = { value -> onUpdate { it.copy(debug = it.debug.copy(statsOverlayEnabled = value)) } },
    )
}

@Composable
private fun WholeSecondsLabel(value: Float): String =
    stringResource(R.string.settings_value_seconds, value.roundToInt().toString())
