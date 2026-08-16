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
import androidx.compose.ui.graphics.vector.ImageVector
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.ui.theme.FileArchiveColor
import com.jonecx.ibex.ui.theme.FileAudioColor
import com.jonecx.ibex.ui.theme.FileDirectoryColor
import com.jonecx.ibex.ui.theme.FileDocumentColor
import com.jonecx.ibex.ui.theme.FileImageColor
import com.jonecx.ibex.ui.theme.FileVideoColor

// The app's canonical icon + theme tint for a file type. Shared so any surface that shows a file (grid,
// list, transfer sheet) draws the same glyph without repeating the mapping.
@Composable
fun fileTypeGlyph(fileType: FileType): Pair<ImageVector, Color> = when (fileType) {
    FileType.DIRECTORY -> Icons.Filled.Folder to FileDirectoryColor
    FileType.IMAGE -> Icons.Filled.Image to FileImageColor
    FileType.VIDEO -> Icons.Filled.VideoFile to FileVideoColor
    FileType.AUDIO -> Icons.Filled.AudioFile to FileAudioColor
    FileType.DOCUMENT -> Icons.Filled.Description to FileDocumentColor
    FileType.ARCHIVE -> Icons.Filled.Archive to FileArchiveColor
    FileType.APK -> Icons.Filled.Android to FileImageColor
    FileType.UNKNOWN -> Icons.AutoMirrored.Filled.InsertDriveFile to MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun FileIcon(
    fileItem: FileItem,
    modifier: Modifier = Modifier,
    // Null marks the icon decorative when a surrounding label already names the item, e.g. a labelled grid tile.
    contentDescription: String? = fileItem.fileType.name,
) {
    val (icon, tint) = fileTypeGlyph(fileItem.fileType)
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}
