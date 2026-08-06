package com.jonecx.ibex.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jonecx.azmaree.player.AzmareePlayer
import com.jonecx.azmaree.player.model.ControlsConfig
import com.jonecx.azmaree.player.model.PlaybackConfig
import com.jonecx.azmaree.player.model.PlayerSettings
import com.jonecx.azmaree.player.model.withMaterialAccent
import com.jonecx.azmaree.source.smb.SmbDataSourceFactory
import com.jonecx.ibex.data.model.FileItem
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
    val settings = remember(smbContextProvider) {
        PlayerSettings(
            playback = PlaybackConfig(
                dataSources = listOf(
                    SmbDataSourceFactory { host -> smbContextProvider.get(host) },
                ),
            ),
            // Tap drives the shared viewer chrome, so keep Azmaree's controls in step instead of auto-hiding.
            controls = ControlsConfig(autoHideDelayMs = ControlsConfig.NEVER_AUTO_HIDE),
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
        settings = settings.copy(style = settings.style.withMaterialAccent()),
        sessionKey = fileItem.path,
    )
}
