package com.jonecx.ibex.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class FileSourceType {
    LOCAL_STORAGE,
    LOCAL_DOWNLOADS,
    LOCAL_IMAGES,
    LOCAL_VIDEOS,
    LOCAL_AUDIO,
    LOCAL_DOCUMENTS,
    LOCAL_APPS,
    LOCAL_RECENT,
    LOCAL_TRASH,
    STORAGE_ANALYSIS,
    CLOUD,
    SMB,
    FTP,
}

data class FileSource(
    val id: String,
    val name: String,
    val type: FileSourceType,
    val icon: ImageVector,
    val iconTint: Color,
    val subtitle: String? = null,
    val isLocal: Boolean = true,
    val isEnabled: Boolean = true,
    val rootPath: String? = null,
)
