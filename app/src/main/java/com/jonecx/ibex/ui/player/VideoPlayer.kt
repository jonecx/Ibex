package com.jonecx.ibex.ui.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jonecx.azmaree.player.AzmareePlayer
import com.jonecx.azmaree.player.model.ControlsConfig
import com.jonecx.azmaree.player.model.PlayerSettings
import com.jonecx.azmaree.source.smb.SmbDataSourceFactory
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.preferences.PlayerSettingsPreferencesContract
import com.jonecx.ibex.data.repository.SmbContextProviderContract
import org.koin.compose.koinInject

// Playback is entirely Azmaree's; Ibex only supplies the smb:// byte source, resolving each host to the
// jcifs context its own SMB browsing already authenticated. onPrevious has no Azmaree control (the pager
// swipe covers it), so it is intentionally unused here.
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
    // Defaults until the store's first emission; the read is fast, so any flash is a single frame.
    val defaults = remember { PlayerSettings() }
    val stored by playerSettingsPreferences.settings.collectAsState(initial = defaults)
    // BrandRed is the primary accent, matching Azmaree's player look.
    val accent = MaterialTheme.colorScheme.primary
    val settings = remember(stored, accent, smbContextProvider) {
        stored.copy(
            playback = stored.playback.copy(
                dataSources = listOf(
                    SmbDataSourceFactory { host -> smbContextProvider.get(host) },
                ),
            ),
            // Tap drives the shared viewer chrome, so keep Azmaree's controls in step instead of auto-hiding.
            controls = stored.controls.copy(autoHideDelayMs = ControlsConfig.NEVER_AUTO_HIDE),
            style = stored.style.copy(
                progressPlayedColor = accent,
                bufferingIndicatorColor = accent,
                errorActionColor = accent,
                statsAccentColor = accent,
            ),
        )
    }
    AzmareePlayer(
        url = fileItem.uri.toString(),
        modifier = modifier,
        playWhenReady = isActive,
        showControls = controlsVisible,
        title = fileItem.name,
        onTap = onToggleControls,
        onNext = onNext,
        settings = settings,
        sessionKey = fileItem.path,
    )
}
