package com.jonecx.ibex.ui.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.buttons.SeekBackButton
import androidx.media3.ui.compose.material3.buttons.SeekForwardButton
import com.jonecx.ibex.R
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
            IconButton(
                onClick = { onPrevious?.invoke() },
                enabled = onPrevious != null,
                colors = scrimButtonColors,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.previous),
                    tint = if (onPrevious != null) White else WhiteSecondary,
                )
            }
            SeekBackButton(player)
            PlayPauseButton(player)
            SeekForwardButton(player)
            IconButton(
                onClick = { onNext?.invoke() },
                enabled = onNext != null,
                colors = scrimButtonColors,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.next),
                    tint = if (onNext != null) White else WhiteSecondary,
                )
            }
        }
    }
}
