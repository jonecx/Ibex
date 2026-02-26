package com.jonecx.ibex.ui.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.ui.theme.Black
import com.jonecx.ibex.ui.theme.ScrimDark
import com.jonecx.ibex.ui.theme.White

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    fileItem: FileItem,
    isActive: Boolean,
    playerFactory: PlayerFactory,
    modifier: Modifier = Modifier,
) {
    val player = remember(fileItem.path) {
        playerFactory.create().apply {
            setMediaItem(MediaItem.fromUri(fileItem.uri))
            prepare()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var videoAspectRatio by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isActive) {
        player.playWhenReady = isActive
    }

    DisposableEffect(fileItem.path) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    videoAspectRatio = size.width.toFloat() / size.height.toFloat()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(
        modifier = modifier.background(Black),
        contentAlignment = Alignment.Center,
    ) {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = if (videoAspectRatio > 0f) {
                Modifier
                    .fillMaxSize()
                    .aspectRatio(videoAspectRatio)
            } else {
                Modifier.fillMaxSize()
            },
        )

        PlaybackControls(
            player = player,
            isPlaying = isPlaying,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        )
    }
}

@Composable
private fun PlaybackControls(
    player: Player,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrimButtonColors = IconButtonDefaults.iconButtonColors(containerColor = ScrimDark)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { player.seekBack() },
            colors = scrimButtonColors,
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = stringResource(R.string.seek_back),
                tint = White,
            )
        }
        IconButton(
            onClick = {
                if (player.isPlaying) player.pause() else player.play()
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(56.dp)
                .background(ScrimDark, CircleShape),
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                tint = White,
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(
            onClick = { player.seekForward() },
            colors = scrimButtonColors,
        ) {
            Icon(
                imageVector = Icons.Filled.Forward10,
                contentDescription = stringResource(R.string.seek_forward),
                tint = White,
            )
        }
    }
}
