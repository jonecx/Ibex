package com.jonecx.ibex.ui.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.buttons.SeekBackButton
import androidx.media3.ui.compose.material3.buttons.SeekForwardButton
import com.jonecx.ibex.R
import com.jonecx.ibex.ui.theme.Black
import com.jonecx.ibex.ui.theme.ScrimDark
import com.jonecx.ibex.ui.theme.White
import com.jonecx.ibex.ui.theme.WhiteSecondary
import com.jonecx.ibex.util.formatDuration

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackControls(
    player: Player,
    currentPosition: Long,
    duration: Long,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scrimButtonColors = IconButtonDefaults.iconButtonColors(containerColor = ScrimDark)
    val sliderColors = SliderDefaults.colors(
        thumbColor = White,
        activeTrackColor = White,
        inactiveTrackColor = WhiteSecondary,
    )
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    val speedLabel = formatSpeed(playbackSpeed)
    val speedContentDescription = stringResource(R.string.playback_speed)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ScrimDark)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val sliderPosition = if (duration > 0) {
            currentPosition.toFloat() / duration.toFloat()
        } else {
            0f
        }

        Slider(
            value = sliderPosition,
            onValueChange = { fraction ->
                player.seekTo((fraction * duration).toLong())
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            colors = sliderColors,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    colors = sliderColors,
                    thumbSize = DpSize(2.dp, 16.dp),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    modifier = Modifier.height(2.dp),
                    sliderState = sliderState,
                    colors = sliderColors,
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 0.dp,
                )
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(currentPosition),
                style = MaterialTheme.typography.labelSmall,
                color = WhiteSecondary,
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = WhiteSecondary,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            SeekBackButton(player)
            Box {
                TextButton(
                    onClick = { speedMenuExpanded = true },
                    modifier = Modifier.semantics {
                        contentDescription = speedContentDescription
                    },
                ) {
                    Text(
                        text = speedLabel,
                        color = White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                DropdownMenu(
                    expanded = speedMenuExpanded,
                    onDismissRequest = { speedMenuExpanded = false },
                    containerColor = Black,
                ) {
                    PLAYBACK_SPEEDS.forEach { speed ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = formatSpeed(speed),
                                    color = if (speed == playbackSpeed) White else WhiteSecondary,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            onClick = {
                                playbackSpeed = speed
                                player.setPlaybackSpeed(speed)
                                speedMenuExpanded = false
                            },
                        )
                    }
                }
            }
            SeekForwardButton(player)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            NavigationButton(
                icon = Icons.Filled.SkipPrevious,
                description = stringResource(R.string.previous),
                onClick = onPrevious,
                colors = scrimButtonColors,
            )
            PlayPauseButton(player)
            NavigationButton(
                icon = Icons.Filled.SkipNext,
                description = stringResource(R.string.next),
                onClick = onNext,
                colors = scrimButtonColors,
            )
        }
    }
}

@Composable
private fun NavigationButton(
    icon: ImageVector,
    description: String,
    onClick: (() -> Unit)?,
    colors: IconButtonColors,
) {
    IconButton(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        colors = colors,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (onClick != null) White else WhiteSecondary,
        )
    }
}

private val PLAYBACK_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

private fun formatSpeed(speed: Float): String {
    return if (speed % 1f == 0f) {
        "${speed.toInt()}X"
    } else {
        "${speed}X"
    }
}
