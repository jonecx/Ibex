package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.ui.theme.AlphaSecondary

private val GridItemShape = RoundedCornerShape(2.dp)

// Prominent play badge for the roomy grid tiles; a fixed size keeps the tile off the SubcomposeLayout path.
private val GridVideoBadgeSize = 40.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    fileItem: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isChecked: Boolean = false,
    onLongClick: () -> Unit = {},
) {
    // A media folder renders its cover photo instead of a plain folder icon; a plain file renders its own thumbnail.
    val coverItem = remember(fileItem) { fileItem.thumbnailCover() }
    // Merge the tile into one spoken label so TalkBack reads "Camera, 42 items" once, not name + count + cover.
    val folderDescription = fileItem.folderContentDescription()

    Column(
        modifier = modifier
            .clip(GridItemShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(selectionBackgroundColor(isSelected))
            .semantics { folderDescription?.let { contentDescription = it } }
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var thumbnailFailed by remember(fileItem.path) { mutableStateOf(false) }
        val showThumbnail = coverItem != null && !thumbnailFailed

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (showThumbnail) {
                ThumbnailImage(
                    fileItem = coverItem,
                    modifier = Modifier.matchParentSize(),
                    // The folder's own badge signals it is a folder, so skip the per-file video play glyph.
                    showVideoIndicator = !fileItem.isDirectory,
                    // Grid tiles are roomy, so use the enlarged badge rather than the list-sized default.
                    videoIndicatorSize = GridVideoBadgeSize,
                    onError = { thumbnailFailed = true },
                )
                if (fileItem.isDirectory) {
                    FolderBadge(modifier = Modifier.align(Alignment.BottomStart).padding(4.dp))
                }
            } else {
                FileIcon(
                    fileItem = fileItem,
                    modifier = Modifier.fillMaxWidth(0.5f).aspectRatio(1f),
                )
            }

            if (isSelectionMode) {
                SelectionCheckmark(
                    isChecked = isChecked,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                )
            }
        }

        // Folders always caption their name and count; plain files only caption when the thumbnail fails to load.
        if (fileItem.isDirectory) {
            TileCaption(text = fileItem.name, color = selectionContentColor(isSelected))
            fileItem.childCount?.let { count ->
                TileCaption(
                    text = stringResource(R.string.items_count, count),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    topPadding = 2.dp,
                )
            }
        } else if (!fileItem.fileType.isViewable || thumbnailFailed) {
            TileCaption(text = fileItem.name, color = selectionContentColor(isSelected))
        }
    }
}

@Composable
private fun TileCaption(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    topPadding: Dp = 6.dp,
) {
    Text(
        text = text,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        color = color,
        modifier = modifier.fillMaxWidth().padding(top = topPadding),
    )
}

// Small translucent folder glyph pinned to a cover so a media-folder tile reads as a folder, not a single photo.
@Composable
private fun FolderBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = AlphaSecondary),
        modifier = modifier.size(20.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            modifier = Modifier.padding(3.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// One spoken label for a folder tile: "name, N items"; null for files, which keep their thumbnail's own label.
@Composable
private fun FileItem.folderContentDescription(): String? = when {
    !isDirectory -> null
    childCount != null -> "$name, ${stringResource(R.string.items_count, childCount)}"
    else -> name
}
