package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.ui.theme.AlphaSecondary
import com.jonecx.ibex.ui.theme.AlphaTileResting

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
    // One spoken label per tile so TalkBack reads "Camera, 42 items" once; the inner icon and frame stay decorative.
    val tileDescription = fileItem.folderContentDescription() ?: fileItem.name

    // A soft resting fill lifts flat icon tiles off the page; hover and selection still layer a stronger tone on top.
    val tileBackground = if (isSelected) {
        selectionBackgroundColor(isSelected = true)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaTileResting)
    }

    Column(
        modifier = modifier
            .clip(GridItemShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(tileBackground)
            .semantics(mergeDescendants = true) { contentDescription = tileDescription }
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
                    // The folder's label strip signals it is a folder, so skip the per-file video play glyph.
                    showVideoIndicator = !fileItem.isDirectory,
                    // Grid tiles are roomy, so use the enlarged badge rather than the list-sized default.
                    videoIndicatorSize = GridVideoBadgeSize,
                    // The tile carries the spoken label, so keep the frame out of the TalkBack tree.
                    contentDescription = null,
                    onError = { thumbnailFailed = true },
                )
                if (fileItem.isDirectory) {
                    // A cover folder overlays its name and count on a translucent strip so the label stays in the tile.
                    FolderCoverLabel(
                        name = fileItem.name,
                        childCount = fileItem.childCount,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            } else if (fileItem.isDirectory) {
                // A plain folder stacks its icon, name, and count inside the square, like the file tiles.
                FileTypeTile(
                    fileItem = fileItem,
                    contentColor = selectionContentColor(isSelected),
                    modifier = Modifier.fillMaxSize(),
                    childCount = fileItem.childCount,
                )
            } else {
                // Non-thumbnail files carry their name inside the square so every cell stays the same height.
                FileTypeTile(
                    fileItem = fileItem,
                    contentColor = selectionContentColor(isSelected),
                    modifier = Modifier.fillMaxSize(),
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
    }
}

// Stacks a type icon over a name (and an optional child count for folders) inside the square, keeping tiles uniform.
@Composable
private fun FileTypeTile(
    fileItem: FileItem,
    contentColor: Color,
    modifier: Modifier = Modifier,
    childCount: Int? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FileIcon(
            fileItem = fileItem,
            modifier = Modifier.fillMaxWidth(0.45f).aspectRatio(1f),
            // The tile carries the spoken label, so keep the icon out of the TalkBack tree.
            contentDescription = null,
        )
        Text(
            text = fileItem.name,
            style = MaterialTheme.typography.bodySmall,
            // A lone name may wrap to two lines; leave room for the count line when a folder shows one.
            maxLines = if (childCount != null) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = contentColor,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        childCount?.let { count ->
            Text(
                text = stringResource(R.string.folder_child_count, count),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
    }
}

// A cover folder's name and count on a translucent strip pinned to the tile bottom, keeping the label inside the tile.
@Composable
private fun FolderCoverLabel(
    name: String,
    childCount: Int?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = AlphaSecondary),
        modifier = modifier.fillMaxWidth().padding(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            childCount?.let { count ->
                Text(
                    text = stringResource(R.string.folder_child_count, count),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

// One spoken label for a folder tile: "name, N items"; null for files, which keep their thumbnail's own label.
@Composable
private fun FileItem.folderContentDescription(): String? = when {
    !isDirectory -> null
    childCount != null -> "$name, ${stringResource(R.string.items_count, childCount)}"
    else -> name
}
