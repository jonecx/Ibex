package com.jonecx.ibex.data.model

import android.os.Environment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.Color

object FileSources {

    fun getLocalSources(
        storage: String,
        downloads: String,
        images: String,
        videos: String,
        audio: String,
        documents: String,
        apps: String,
        recent: String,
        analysis: String,
        trash: String,
    ): List<FileSource> = listOf(
        FileSource(
            id = "main_storage",
            name = storage,
            type = FileSourceType.LOCAL_STORAGE,
            icon = Icons.Filled.Storage,
            iconTint = Color(0xFF546E7A),
            rootPath = Environment.getExternalStorageDirectory().absolutePath,
        ),
        FileSource(
            id = "downloads",
            name = downloads,
            type = FileSourceType.LOCAL_DOWNLOADS,
            icon = Icons.Filled.Download,
            iconTint = Color(0xFFF57C00),
            rootPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
        ),
        FileSource(
            id = "images",
            name = images,
            type = FileSourceType.LOCAL_IMAGES,
            icon = Icons.Filled.Image,
            iconTint = Color(0xFFAB47BC),
        ),
        FileSource(
            id = "videos",
            name = videos,
            type = FileSourceType.LOCAL_VIDEOS,
            icon = Icons.Filled.VideoFile,
            iconTint = Color(0xFFEC407A),
        ),
        FileSource(
            id = "audio",
            name = audio,
            type = FileSourceType.LOCAL_AUDIO,
            icon = Icons.Filled.AudioFile,
            iconTint = Color(0xFF26C6DA),
        ),
        FileSource(
            id = "documents",
            name = documents,
            type = FileSourceType.LOCAL_DOCUMENTS,
            icon = Icons.Filled.Description,
            iconTint = Color(0xFF1976D2),
        ),
        FileSource(
            id = "apps",
            name = apps,
            type = FileSourceType.LOCAL_APPS,
            icon = Icons.Filled.Android,
            iconTint = Color(0xFF388E3C),
        ),
        FileSource(
            id = "recent",
            name = recent,
            type = FileSourceType.LOCAL_RECENT,
            icon = Icons.Filled.Schedule,
            iconTint = Color(0xFF78909C),
        ),
        FileSource(
            id = "storage_analysis",
            name = analysis,
            type = FileSourceType.STORAGE_ANALYSIS,
            icon = Icons.Filled.PieChart,
            iconTint = Color(0xFF8D6E63),
        ),
        FileSource(
            id = "trash",
            name = trash,
            type = FileSourceType.LOCAL_TRASH,
            icon = Icons.Filled.Delete,
            iconTint = Color(0xFFD32F2F),
        ),
    )

    fun getRemoteSources(
        cloud: String,
        smb: String,
        ftp: String,
        comingSoon: String,
    ): List<FileSource> = listOf(
        FileSource(
            id = "cloud",
            name = cloud,
            type = FileSourceType.CLOUD,
            icon = Icons.Filled.Cloud,
            iconTint = Color(0xFF42A5F5),
            isLocal = false,
            isEnabled = false,
            subtitle = comingSoon,
        ),
        FileSource(
            id = "smb",
            name = smb,
            type = FileSourceType.SMB,
            icon = Icons.Filled.Lan,
            iconTint = Color(0xFFE64A19),
            isLocal = false,
            isEnabled = false,
            subtitle = comingSoon,
        ),
        FileSource(
            id = "ftp",
            name = ftp,
            type = FileSourceType.FTP,
            icon = Icons.Filled.Folder,
            iconTint = Color(0xFF26A69A),
            isLocal = false,
            isEnabled = false,
            subtitle = comingSoon,
        ),
    )
}
