package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType

private val GridItemShape = RoundedCornerShape(2.dp)

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
    Column(
        modifier = modifier
            .clip(GridItemShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val isMediaType = fileItem.fileType == FileType.IMAGE || fileItem.fileType == FileType.VIDEO
        var thumbnailFailed by remember(fileItem.path) { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (isMediaType && !thumbnailFailed) {
                ThumbnailImage(
                    fileItem = fileItem,
                    modifier = Modifier.matchParentSize(),
                    onError = { thumbnailFailed = true },
                )
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

        if (!isMediaType || thumbnailFailed) {
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
    }
}
