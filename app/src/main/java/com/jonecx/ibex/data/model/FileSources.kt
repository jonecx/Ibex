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
import com.jonecx.ibex.ui.theme.SourceAnalysisColor
import com.jonecx.ibex.ui.theme.SourceAppsColor
import com.jonecx.ibex.ui.theme.SourceAudioColor
import com.jonecx.ibex.ui.theme.SourceCloudColor
import com.jonecx.ibex.ui.theme.SourceDocumentsColor
import com.jonecx.ibex.ui.theme.SourceDownloadsColor
import com.jonecx.ibex.ui.theme.SourceFtpColor
import com.jonecx.ibex.ui.theme.SourceImagesColor
import com.jonecx.ibex.ui.theme.SourceRecentColor
import com.jonecx.ibex.ui.theme.SourceSmbColor
import com.jonecx.ibex.ui.theme.SourceStorageColor
import com.jonecx.ibex.ui.theme.SourceTrashColor
import com.jonecx.ibex.ui.theme.SourceVideosColor

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
            iconTint = SourceStorageColor,
            rootPath = Environment.getExternalStorageDirectory().absolutePath,
        ),
        FileSource(
            id = "downloads",
            name = downloads,
            type = FileSourceType.LOCAL_DOWNLOADS,
            icon = Icons.Filled.Download,
            iconTint = SourceDownloadsColor,
            rootPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
        ),
        FileSource(
            id = "images",
            name = images,
            type = FileSourceType.LOCAL_IMAGES,
            icon = Icons.Filled.Image,
            iconTint = SourceImagesColor,
        ),
        FileSource(
            id = "videos",
            name = videos,
            type = FileSourceType.LOCAL_VIDEOS,
            icon = Icons.Filled.VideoFile,
            iconTint = SourceVideosColor,
        ),
        FileSource(
            id = "audio",
            name = audio,
            type = FileSourceType.LOCAL_AUDIO,
            icon = Icons.Filled.AudioFile,
            iconTint = SourceAudioColor,
        ),
        FileSource(
            id = "documents",
            name = documents,
            type = FileSourceType.LOCAL_DOCUMENTS,
            icon = Icons.Filled.Description,
            iconTint = SourceDocumentsColor,
        ),
        FileSource(
            id = "apps",
            name = apps,
            type = FileSourceType.LOCAL_APPS,
            icon = Icons.Filled.Android,
            iconTint = SourceAppsColor,
        ),
        FileSource(
            id = "recent",
            name = recent,
            type = FileSourceType.LOCAL_RECENT,
            icon = Icons.Filled.Schedule,
            iconTint = SourceRecentColor,
        ),
        FileSource(
            id = "storage_analysis",
            name = analysis,
            type = FileSourceType.STORAGE_ANALYSIS,
            icon = Icons.Filled.PieChart,
            iconTint = SourceAnalysisColor,
        ),
        FileSource(
            id = "trash",
            name = trash,
            type = FileSourceType.LOCAL_TRASH,
            icon = Icons.Filled.Delete,
            iconTint = SourceTrashColor,
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
            iconTint = SourceCloudColor,
            isLocal = false,
            isEnabled = false,
            subtitle = comingSoon,
        ),
        FileSource(
            id = "smb",
            name = smb,
            type = FileSourceType.SMB,
            icon = Icons.Filled.Lan,
            iconTint = SourceSmbColor,
            isLocal = false,
            isEnabled = false,
            subtitle = comingSoon,
        ),
        FileSource(
            id = "ftp",
            name = ftp,
            type = FileSourceType.FTP,
            icon = Icons.Filled.Folder,
            iconTint = SourceFtpColor,
            isLocal = false,
            isEnabled = false,
            subtitle = comingSoon,
        ),
    )
}
