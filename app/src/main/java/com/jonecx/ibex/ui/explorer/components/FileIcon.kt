package com.jonecx.ibex.ui.explorer.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType

@Composable
fun FileIcon(
    fileItem: FileItem,
    modifier: Modifier = Modifier,
    showThumbnail: Boolean = true,
) {
    val context = LocalContext.current

    when {
        fileItem.fileType == FileType.IMAGE && showThumbnail -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(fileItem.path)
                    .crossfade(true)
                    .build(),
                contentDescription = fileItem.name,
                modifier = modifier,
                contentScale = ContentScale.Crop,
            )
        }
        fileItem.fileType == FileType.VIDEO && showThumbnail -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(fileItem.path)
                    .videoFrameMillis(1000)
                    .crossfade(true)
                    .build(),
                contentDescription = fileItem.name,
                modifier = modifier,
                contentScale = ContentScale.Crop,
            )
        }
        else -> {
            val (icon, tint) = when (fileItem.fileType) {
                FileType.DIRECTORY -> Icons.Filled.Folder to Color(0xFFFFB74D)
                FileType.IMAGE -> Icons.Filled.Image to Color(0xFF4CAF50)
                FileType.VIDEO -> Icons.Filled.VideoFile to Color(0xFFE91E63)
                FileType.AUDIO -> Icons.Filled.AudioFile to Color(0xFF9C27B0)
                FileType.DOCUMENT -> Icons.Filled.Description to Color(0xFF2196F3)
                FileType.ARCHIVE -> Icons.Filled.Archive to Color(0xFF795548)
                FileType.APK -> Icons.Filled.Android to Color(0xFF4CAF50)
                FileType.UNKNOWN -> Icons.AutoMirrored.Filled.InsertDriveFile to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                imageVector = icon,
                contentDescription = fileItem.fileType.name,
                modifier = modifier,
                tint = tint,
            )
        }
    }
}
