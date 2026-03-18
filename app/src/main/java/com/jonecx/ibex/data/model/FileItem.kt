package com.jonecx.ibex.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

enum class FileType {
    DIRECTORY,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    APK,
    UNKNOWN,
    ;

    val isImage: Boolean get() = this == IMAGE
    val isVideo: Boolean get() = this == VIDEO
    val isViewable: Boolean get() = isImage || isVideo
}

@Immutable
data class FileItem(
    val name: String,
    val path: String,
    val uri: Uri,
    val size: Long,
    val lastModified: Long,
    val createdAt: Long = 0L,
    val isDirectory: Boolean,
    val fileType: FileType,
    val mimeType: String? = null,
    val childCount: Int? = null,
    val isRemote: Boolean = false,
) {
    companion object
}
