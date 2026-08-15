package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jonecx.azmaree.image.AzmareeImage
import com.jonecx.azmaree.image.model.AzmareeImageState
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.ui.theme.AlphaSecondary
import com.jonecx.ibex.ui.util.previewPlaceholder

// Placeholder icon fills half the tile, matching the grid's fallback FileIcon.
private const val PlaceholderIconFraction = 0.5f

// Default badge keeps the list thumbnail's original 24dp glyph; the grid passes a larger, tile-proportional size.
private val DefaultVideoBadgeSize = 24.dp

// Glyph sits inside the badge by size/6 (4dp at the 24dp default), reproducing the original padding.
private const val VideoGlyphInsetDivisor = 6f

@Composable
fun ThumbnailImage(
    fileItem: FileItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(2.dp),
    showVideoIndicator: Boolean = true,
    videoIndicatorSize: Dp = DefaultVideoBadgeSize,
    // Null marks the frame decorative when a surrounding label already names the item, e.g. a labelled grid tile.
    contentDescription: String? = fileItem.name,
    onError: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val factory = LocalFileImageRequestFactory.current

    val imageRequest = remember(fileItem.path) { factory.create(context, fileItem) }
    // Show the file-type icon until a frame arrives; previews have no async load, so treat them as already loaded.
    var frameLoaded by remember(fileItem.path) { mutableStateOf(false) }
    val loaded = LocalInspectionMode.current || frameLoaded

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Sits behind the frame as the loading, error, and no-thumbnail fallback; the loaded frame covers it.
        // Decorative: the thumbnail below already carries the file name, so keep this out of the TalkBack tree.
        FileIcon(
            fileItem = fileItem,
            modifier = Modifier
                .fillMaxWidth(PlaceholderIconFraction)
                .aspectRatio(1f)
                .clearAndSetSemantics {},
        )
        AzmareeImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .previewPlaceholder(fileItem.fileType),
            contentScale = contentScale,
            onState = { state ->
                when (state) {
                    is AzmareeImageState.Success -> frameLoaded = true
                    is AzmareeImageState.Error -> onError?.invoke()
                    else -> Unit
                }
            },
        )
        // Only badge an actual frame; before load the video FileIcon already signals the type.
        if (showVideoIndicator && loaded && fileItem.fileType.isVideo) {
            VideoPlayBadge(size = videoIndicatorSize)
        }
    }
}

// Translucent circular play badge over a video frame; larger in the grid, the original 24dp in list rows.
@Composable
private fun VideoPlayBadge(size: Dp, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = AlphaSecondary),
        modifier = modifier.size(size),
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(size / VideoGlyphInsetDivisor),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// The item to draw a thumbnail from: a media folder resolves to its cover; a viewable file resolves to itself;
// anything else (a plain folder or non-media file) has no thumbnail and returns null.
fun FileItem.thumbnailCover(): FileItem? = when {
    isDirectory -> coverPath?.let {
        copy(path = it, fileType = if (coverIsVideo) FileType.VIDEO else FileType.IMAGE)
    }
    fileType.isViewable -> this
    else -> null
}
