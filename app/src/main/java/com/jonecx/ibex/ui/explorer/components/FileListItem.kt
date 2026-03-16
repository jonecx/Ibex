package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.util.formatDate
import com.jonecx.ibex.util.formatFileSize

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    fileItem: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isChecked: Boolean = false,
    onLongClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(selectionBackgroundColor(isSelected))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelectionMode) {
            SelectionCheckmark(isChecked = isChecked)
            Spacer(modifier = Modifier.width(12.dp))
        }

        var thumbnailFailed by remember(fileItem.path) { mutableStateOf(false) }
        val showThumbnail = !thumbnailFailed && fileItem.fileType.isViewable
        if (showThumbnail) {
            ThumbnailImage(
                fileItem = fileItem,
                modifier = Modifier.size(48.dp),
                onError = { thumbnailFailed = true },
            )
        } else {
            FileIcon(
                fileItem = fileItem,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = selectionContentColor(isSelected),
            )
            Text(
                text = if (fileItem.isDirectory) {
                    stringResource(R.string.items_count, fileItem.childCount ?: 0)
                } else {
                    "${formatFileSize(fileItem.size)} • ${formatDate(fileItem.lastModified)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    selectionContentColor(true).copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
