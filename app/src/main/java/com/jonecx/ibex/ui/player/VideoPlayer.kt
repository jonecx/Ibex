package com.jonecx.ibex.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jonecx.azmaree.player.AzmareePlayer
import com.jonecx.azmaree.player.model.ControlsConfig
import com.jonecx.azmaree.player.model.PlayerSettings
import com.jonecx.azmaree.player.model.PlayerTelemetry
import com.jonecx.azmaree.player.model.withMaterialAccent
import com.jonecx.azmaree.source.smb.SmbDataSourceFactory
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.preferences.PlayerSettingsPreferencesContract
import com.jonecx.ibex.data.repository.SmbContextProviderContract
import org.koin.compose.koinInject

// Playback is entirely Azmaree's; Ibex only supplies the smb:// byte source, resolving each host to the
// jcifs context its own SMB browsing already authenticated.
@Composable
fun VideoPlayer(
    fileItem: FileItem,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    controlsVisible: Boolean = true,
    onToggleControls: () -> Unit = {},
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
) {
    val smbContextProvider = koinInject<SmbContextProviderContract>()
    val playerSettingsPreferences = koinInject<PlayerSettingsPreferencesContract>()
    val telemetry = koinInject<PlayerTelemetry>()
    // Defaults until the store's first emission; the read is fast, so any flash is a single frame.
    val defaults = remember { PlayerSettings() }
    val stored by playerSettingsPreferences.settings.collectAsState(initial = defaults)
    // BrandRed is the primary accent; the SDK helper paints every accent surface, thumb-arc dial included.
    val accentStyle = stored.style.withMaterialAccent()
    val settings = remember(stored, accentStyle, smbContextProvider) {
        stored.copy(
            playback = stored.playback.copy(
                dataSources = listOf(
                    SmbDataSourceFactory { host -> smbContextProvider.get(host) },
                ),
            ),
            // Tap drives the shared viewer chrome, so keep Azmaree's controls in step instead of auto-hiding.
            controls = stored.controls.copy(autoHideDelayMs = ControlsConfig.NEVER_AUTO_HIDE),
            style = accentStyle,
        )
    }
    AzmareePlayer(
        url = fileItem.uri.toString(),
        modifier = modifier,
        playWhenReady = isActive,
        showControls = controlsVisible,
        title = fileItem.name,
        onTap = onToggleControls,
        onPrevious = onPrevious,
        onNext = onNext,
        settings = settings,
        telemetry = telemetry,
        sessionKey = fileItem.path,
    )
}
