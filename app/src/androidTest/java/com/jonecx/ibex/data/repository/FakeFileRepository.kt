package com.jonecx.ibex.data.repository

import android.net.Uri
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeFileRepository : FileRepository {

    private val storageFiles = listOf(
        createFolder("Alarms", 0),
        createFolder("Android", 3),
        createFolder("Audiobooks", 0),
        createFolder("DCIM", 0),
        createFolder("Documents", 0),
        createFolder("Download", 15),
    )

    private val downloadFiles = listOf(
        createFile("Audio 1.mp3", 121651, FileType.AUDIO),
        createFile("Audio Flac 1.zip", 16672358, FileType.ARCHIVE),
        createFile("Coffee.jpg", 1677722, FileType.IMAGE),
        createFile("CS201-DS.ppt", 1153434, FileType.DOCUMENT),
        createFile("CS8391-DS.docx", 50585, FileType.DOCUMENT),
        createFile("Headphone.mp4", 115343360, FileType.VIDEO),
        createFile("PDF_DS.pdf", 173015, FileType.DOCUMENT),
    )

    private val imageFiles = listOf(
        createMediaFile("Sunset.jpg", 2500000, FileType.IMAGE),
        createMediaFile("Portrait.png", 1800000, FileType.IMAGE),
        createMediaFile("Screenshot_2024.png", 500000, FileType.IMAGE),
    )

    private val videoFiles = listOf(
        createMediaFile("Birthday.mp4", 150000000, FileType.VIDEO),
        createMediaFile("Tutorial.mkv", 80000000, FileType.VIDEO),
    )

    private val audioFiles = listOf(
        createMediaFile("Song1.mp3", 5000000, FileType.AUDIO),
        createMediaFile("Podcast.m4a", 25000000, FileType.AUDIO),
        createMediaFile("Ringtone.ogg", 200000, FileType.AUDIO),
    )

    private val documentFiles = listOf(
        createMediaFile("Report.pdf", 1500000, FileType.DOCUMENT),
        createMediaFile("Notes.txt", 5000, FileType.DOCUMENT),
        createMediaFile("Spreadsheet.xlsx", 250000, FileType.DOCUMENT),
    )

    private val appFiles = listOf(
        createAppFile("Calculator", "com.android.calculator"),
        createAppFile("Camera", "com.android.camera"),
        createAppFile("Settings", "com.android.settings"),
    )

    private val recentFiles = listOf(
        createMediaFile("RecentDoc.pdf", 100000, FileType.DOCUMENT),
        createMediaFile("RecentPhoto.jpg", 2000000, FileType.IMAGE),
    )

    private val trashFiles = listOf(
        createMediaFile("DeletedFile.txt", 1000, FileType.DOCUMENT),
        createMediaFile("OldPhoto.jpg", 3000000, FileType.IMAGE),
    )

    override fun getFiles(path: String): Flow<List<FileItem>> {
        return when {
            path.contains("Download", ignoreCase = true) -> flowOf(downloadFiles)
            path.contains("Image", ignoreCase = true) -> flowOf(imageFiles)
            path.contains("Video", ignoreCase = true) -> flowOf(videoFiles)
            path.contains("Audio", ignoreCase = true) -> flowOf(audioFiles)
            path.contains("Document", ignoreCase = true) -> flowOf(documentFiles)
            path.contains("App", ignoreCase = true) -> flowOf(appFiles)
            path.contains("Recent", ignoreCase = true) -> flowOf(recentFiles)
            path.contains("Trash", ignoreCase = true) -> flowOf(trashFiles)
            else -> flowOf(storageFiles)
        }
    }

    override fun getStorageRoots(): Flow<List<FileItem>> {
        return flowOf(storageFiles)
    }

    override suspend fun getFileDetails(path: String): FileItem? {
        return storageFiles.find { it.path == path }
            ?: downloadFiles.find { it.path == path }
    }

    private fun createFolder(name: String, childCount: Int): FileItem {
        return FileItem(
            name = name,
            path = "/storage/emulated/0/$name",
            uri = Uri.parse("file:///storage/emulated/0/$name"),
            size = 0,
            lastModified = System.currentTimeMillis(),
            isDirectory = true,
            fileType = FileType.DIRECTORY,
            childCount = childCount,
        )
    }

    private fun createFile(name: String, size: Long, fileType: FileType): FileItem {
        return FileItem(
            name = name,
            path = "/storage/emulated/0/Download/$name",
            uri = Uri.parse("file:///storage/emulated/0/Download/$name"),
            size = size,
            lastModified = System.currentTimeMillis(),
            isDirectory = false,
            fileType = fileType,
        )
    }

    private fun createMediaFile(name: String, size: Long, fileType: FileType): FileItem {
        return FileItem(
            name = name,
            path = "/storage/emulated/0/$name",
            uri = Uri.parse("file:///storage/emulated/0/$name"),
            size = size,
            lastModified = System.currentTimeMillis(),
            isDirectory = false,
            fileType = fileType,
        )
    }

    private fun createAppFile(name: String, packageName: String): FileItem {
        return FileItem(
            name = name,
            path = packageName,
            uri = Uri.parse("package:$packageName"),
            size = 0,
            lastModified = System.currentTimeMillis(),
            isDirectory = false,
            fileType = FileType.APK,
        )
    }
}
