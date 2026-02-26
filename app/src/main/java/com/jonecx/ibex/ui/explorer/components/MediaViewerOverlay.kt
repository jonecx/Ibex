package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.ui.player.PlayerFactory
import com.jonecx.ibex.ui.player.VideoPlayer
import com.jonecx.ibex.ui.theme.Black
import com.jonecx.ibex.ui.theme.ScrimDark
import com.jonecx.ibex.ui.theme.White
import com.jonecx.ibex.ui.theme.WhiteSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerOverlay(
    viewableFiles: List<FileItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    playerFactory: PlayerFactory,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { viewableFiles.size },
    )

    val currentFile = viewableFiles.getOrNull(pagerState.settledPage)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .systemBarsPadding(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { viewableFiles[it].path },
        ) { page ->
            val fileItem = viewableFiles[page]
            when (fileItem.fileType) {
                FileType.VIDEO -> {
                    VideoPlayer(
                        fileItem = fileItem,
                        isActive = pagerState.settledPage == page,
                        playerFactory = playerFactory,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    ZoomableImage(
                        fileItem = fileItem,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        TopAppBar(
            title = {
                Column {
                    Text(
                        text = currentFile?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = White,
                        maxLines = 1,
                    )
                    Text(
                        text = "${pagerState.settledPage + 1} / ${viewableFiles.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WhiteSecondary,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_viewer),
                        tint = White,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ScrimDark,
            ),
        )
    }
}
