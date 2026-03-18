package com.jonecx.ibex.data.repository

import android.net.Uri
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeFileRepository : FileRepository {

    private val storageFiles = listOf(
        createFileItem("Alarms", "/storage/emulated/0/Alarms", isDirectory = true, fileType = FileType.DIRECTORY, childCount = 0),
        createFileItem("Android", "/storage/emulated/0/Android", isDirectory = true, fileType = FileType.DIRECTORY, childCount = 3),
        createFileItem("Audiobooks", "/storage/emulated/0/Audiobooks", isDirectory = true, fileType = FileType.DIRECTORY, childCount = 0),
        createFileItem("DCIM", "/storage/emulated/0/DCIM", isDirectory = true, fileType = FileType.DIRECTORY, childCount = 0),
        createFileItem("Documents", "/storage/emulated/0/Documents", isDirectory = true, fileType = FileType.DIRECTORY, childCount = 0),
        createFileItem("Download", "/storage/emulated/0/Download", isDirectory = true, fileType = FileType.DIRECTORY, childCount = 15),
    )

    private val downloadFiles = listOf(
        createFileItem("Audio 1.mp3", "/storage/emulated/0/Download/Audio 1.mp3", 121651, fileType = FileType.AUDIO),
        createFileItem("Audio Flac 1.zip", "/storage/emulated/0/Download/Audio Flac 1.zip", 16672358, fileType = FileType.ARCHIVE),
        createFileItem("Coffee.jpg", "/storage/emulated/0/Download/Coffee.jpg", 1677722, fileType = FileType.IMAGE),
        createFileItem("CS201-DS.ppt", "/storage/emulated/0/Download/CS201-DS.ppt", 1153434, fileType = FileType.DOCUMENT),
        createFileItem("CS8391-DS.docx", "/storage/emulated/0/Download/CS8391-DS.docx", 50585, fileType = FileType.DOCUMENT),
        createFileItem("Headphone.mp4", "/storage/emulated/0/Download/Headphone.mp4", 115343360, fileType = FileType.VIDEO),
        createFileItem("PDF_DS.pdf", "/storage/emulated/0/Download/PDF_DS.pdf", 173015, fileType = FileType.DOCUMENT),
    )

    private val imageFiles = listOf(
        createFileItem("Sunset.jpg", "/storage/emulated/0/Sunset.jpg", 2500000, fileType = FileType.IMAGE),
        createFileItem("Portrait.png", "/storage/emulated/0/Portrait.png", 1800000, fileType = FileType.IMAGE),
        createFileItem("Screenshot_2024.png", "/storage/emulated/0/Screenshot_2024.png", 500000, fileType = FileType.IMAGE),
    )

    private val videoFiles = listOf(
        createFileItem("Birthday.mp4", "/storage/emulated/0/Birthday.mp4", 150000000, fileType = FileType.VIDEO),
        createFileItem("Tutorial.mkv", "/storage/emulated/0/Tutorial.mkv", 80000000, fileType = FileType.VIDEO),
    )

    private val audioFiles = listOf(
        createFileItem("Song1.mp3", "/storage/emulated/0/Song1.mp3", 5000000, fileType = FileType.AUDIO),
        createFileItem("Podcast.m4a", "/storage/emulated/0/Podcast.m4a", 25000000, fileType = FileType.AUDIO),
        createFileItem("Ringtone.ogg", "/storage/emulated/0/Ringtone.ogg", 200000, fileType = FileType.AUDIO),
    )

    private val documentFiles = listOf(
        createFileItem("Report.pdf", "/storage/emulated/0/Report.pdf", 1500000, fileType = FileType.DOCUMENT),
        createFileItem("Notes.txt", "/storage/emulated/0/Notes.txt", 5000, fileType = FileType.DOCUMENT),
        createFileItem("Spreadsheet.xlsx", "/storage/emulated/0/Spreadsheet.xlsx", 250000, fileType = FileType.DOCUMENT),
    )

    private val appFiles = listOf(
        createFileItem("Calculator", "com.android.calculator", fileType = FileType.APK),
        createFileItem("Camera", "com.android.camera", fileType = FileType.APK),
        createFileItem("Settings", "com.android.settings", fileType = FileType.APK),
    )

    private val recentFiles = listOf(
        createFileItem("RecentDoc.pdf", "/storage/emulated/0/RecentDoc.pdf", 100000, fileType = FileType.DOCUMENT),
        createFileItem("RecentPhoto.jpg", "/storage/emulated/0/RecentPhoto.jpg", 2000000, fileType = FileType.IMAGE),
    )

    private val trashFiles = listOf(
        createFileItem("DeletedFile.txt", "/storage/emulated/0/DeletedFile.txt", 1000, fileType = FileType.DOCUMENT),
        createFileItem("OldPhoto.jpg", "/storage/emulated/0/OldPhoto.jpg", 3000000, fileType = FileType.IMAGE),
    )

    private val dcimFiles = (1..14).map { i ->
        createFileItem("IMG_${i.toString().padStart(4, '0')}.jpg", "/storage/emulated/0/DCIM/IMG_${i.toString().padStart(4, '0')}.jpg", 2_500_000L * i, fileType = FileType.IMAGE)
    } + createFileItem("Camera", "/storage/emulated/0/DCIM/Camera", isDirectory = true, fileType = FileType.DIRECTORY, childCount = 5)

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
            path.contains("DCIM", ignoreCase = true) -> flowOf(dcimFiles)
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

    private fun createFileItem(
        name: String,
        path: String,
        size: Long = 0,
        isDirectory: Boolean = false,
        fileType: FileType = FileType.UNKNOWN,
        childCount: Int? = null,
    ): FileItem = FileItem(
        name = name,
        path = path,
        uri = Uri.parse(if (path.startsWith("/")) "file://$path" else "package:$path"),
        size = size,
        lastModified = System.currentTimeMillis(),
        isDirectory = isDirectory,
        fileType = fileType,
        childCount = childCount,
    )
}
