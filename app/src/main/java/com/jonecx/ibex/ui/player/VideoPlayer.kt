package com.jonecx.ibex.ui.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.jonecx.ibex.ui.components.LoadingView
import com.jonecx.ibex.ui.theme.Black
import com.jonecx.ibex.ui.theme.White
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    fileItem: FileItem,
    isActive: Boolean,
    playerFactory: PlayerFactory,
    modifier: Modifier = Modifier,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
) {
    val player = remember(fileItem.path) {
        playerFactory.create().apply {
            setMediaItem(MediaItem.fromUri(fileItem.uri))
            prepare()
        }
    }

    var videoAspectRatio by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }

    LaunchedEffect(isActive) {
        player.playWhenReady = isActive
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            delay(POSITION_UPDATE_INTERVAL_MS)
        }
    }

    DisposableEffect(fileItem.path) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    videoAspectRatio = size.width.toFloat() / size.height.toFloat()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                currentPosition = player.currentPosition.coerceAtLeast(0L)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val surfaceModifier = remember(videoAspectRatio) {
        if (videoAspectRatio > 0f) {
            Modifier
                .fillMaxSize()
                .aspectRatio(videoAspectRatio)
        } else {
            Modifier.fillMaxSize()
        }
    }

    Box(
        modifier = modifier.background(Black),
        contentAlignment = Alignment.Center,
    ) {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = surfaceModifier,
        )

        if (isBuffering) {
            LoadingView(
                color = White,
                description = stringResource(R.string.video_loading),
            )
        }

        PlaybackControls(
            player = player,
            currentPosition = currentPosition,
            duration = duration,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

private const val POSITION_UPDATE_INTERVAL_MS = 200L
