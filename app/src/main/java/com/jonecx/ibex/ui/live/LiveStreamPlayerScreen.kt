package com.jonecx.ibex.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.jonecx.azmaree.player.AzmareePlayer
import com.jonecx.azmaree.player.model.PlayerSettings
import com.jonecx.azmaree.player.model.PlayerTelemetry
import com.jonecx.ibex.R
import com.jonecx.ibex.data.preferences.PlayerSettingsPreferencesContract
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// Full-screen live feed: a vertical pager over the saved streams (swipe up/down = next/prev), only the
// settled page plays. Playback is entirely Azmaree's; streams are plain HLS/DASH/progressive URLs.
@Composable
fun LiveStreamPlayerScreen(
    startIndex: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveFeedViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val streams = uiState.streams

    if (streams.isEmpty()) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    val playerSettingsPreferences = koinInject<PlayerSettingsPreferencesContract>()
    val telemetry = koinInject<PlayerTelemetry>()
    val accent = MaterialTheme.colorScheme.primary
    val defaults = remember { PlayerSettings() }
    val stored by playerSettingsPreferences.settings.collectAsState(initial = defaults)
    val settings = remember(stored, accent) {
        stored.copy(
            style = stored.style.copy(
                progressPlayedColor = accent,
                bufferingIndicatorColor = accent,
                errorActionColor = accent,
                statsAccentColor = accent,
            ),
        )
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, streams.lastIndex),
        pageCount = { streams.size },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { streams[it].id },
        ) { page ->
            val feed = streams[page]
            AzmareePlayer(
                url = feed.url,
                modifier = Modifier.fillMaxSize(),
                playWhenReady = page == pagerState.settledPage,
                showControls = true,
                title = feed.title,
                description = feed.description.ifBlank { null },
                thumbnailUrl = feed.thumbnailUrl.ifBlank { null },
                settings = settings,
                telemetry = telemetry,
                sessionKey = feed.id,
            )
        }

        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .systemBarsPadding(),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.live_close),
                tint = Color.White,
            )
        }
    }
}
