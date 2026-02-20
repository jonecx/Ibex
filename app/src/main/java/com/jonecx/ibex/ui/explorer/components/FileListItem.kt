package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.util.formatDate
import com.jonecx.ibex.util.formatFileSize

@Composable
fun FileListItem(
    fileItem: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (fileItem.fileType) {
            FileType.IMAGE, FileType.VIDEO -> ThumbnailImage(
                fileItem = fileItem,
                modifier = Modifier.size(48.dp),
            )
            else -> FileIcon(
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
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = if (fileItem.isDirectory) {
                    "${fileItem.childCount ?: 0} items"
                } else {
                    "${formatFileSize(fileItem.size)} • ${formatDate(fileItem.lastModified)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
