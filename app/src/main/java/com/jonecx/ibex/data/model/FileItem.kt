package com.jonecx.ibex.data.model

import android.net.Uri

enum class FileType {
    DIRECTORY,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    APK,
    UNKNOWN,
}

data class FileItem(
    val name: String,
    val path: String,
    val uri: Uri,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val fileType: FileType,
    val mimeType: String? = null,
    val childCount: Int? = null,
)
