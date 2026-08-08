package com.jonecx.ibex.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.VideoFeed
import com.jonecx.ibex.ui.components.ConfirmationDialog
import com.jonecx.ibex.ui.components.EmptyView
import com.jonecx.ibex.ui.components.IbexTopAppBar
import com.jonecx.ibex.ui.theme.AlphaTintBackground
import com.jonecx.ibex.ui.theme.SourceLiveColor
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveFeedScreen(
    onNavigateBack: () -> Unit,
    onAddStream: () -> Unit,
    onEditStream: (VideoFeed) -> Unit,
    onStreamSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveFeedViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var streamToDelete by remember { mutableStateOf<VideoFeed?>(null) }

    Scaffold(
        topBar = {
            IbexTopAppBar(
                title = stringResource(R.string.live_feed_title),
                onNavigateBack = onNavigateBack,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddStream) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.live_add_stream),
                )
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        if (uiState.streams.isEmpty()) {
            EmptyView(
                modifier = Modifier.padding(paddingValues),
                message = stringResource(R.string.live_no_streams),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.streams, key = { it.id }) { stream ->
                    val index = uiState.streams.indexOf(stream)
                    LiveStreamTile(
                        stream = stream,
                        onClick = { onStreamSelected(index) },
                        onEditClick = { onEditStream(stream) },
                        onDeleteClick = { streamToDelete = stream },
                    )
                }
            }
        }
    }

    streamToDelete?.let { stream ->
        ConfirmationDialog(
            title = stringResource(R.string.live_delete_title),
            message = stringResource(R.string.live_delete_message, stream.title),
            confirmText = stringResource(R.string.live_delete_confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.removeStream(stream.id)
                streamToDelete = null
            },
            onDismiss = { streamToDelete = null },
        )
    }
}

@Composable
private fun LiveStreamTile(
    stream: VideoFeed,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (stream.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = stream.thumbnailUrl,
                    contentDescription = stream.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SourceLiveColor.copy(alpha = AlphaTintBackground)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.LiveTv,
                        contentDescription = null,
                        tint = SourceLiveColor,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            // Bottom scrim so the title stays legible over any thumbnail.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.6f),
                        ),
                    ),
            )

            Text(
                text = stream.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            )

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.live_stream_options),
                        tint = Color.White,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.live_edit_stream)) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEditClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.live_delete_stream)) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        },
                    )
                }
            }
        }
    }
}
