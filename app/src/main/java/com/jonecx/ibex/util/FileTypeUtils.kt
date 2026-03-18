package com.jonecx.ibex.util

import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import java.io.File

/**
 * Utility object for determining file types and mime types.
 * Uses Android's MimeTypeMap for mime type detection.
 */
object FileTypeUtils {

    const val THUMBNAIL_SIZE_PX = 300
    const val JPEG_MIME_TYPE = "image/jpeg"
    const val JPEG_QUALITY = 80
    const val IO_BUFFER_SIZE = 64 * 1024
    const val VIDEO_FRAME_TIME_MS = 1000L
    const val VIDEO_FRAME_TIME_US = VIDEO_FRAME_TIME_MS * 1000
    const val SECONDS_TO_MILLIS = 1000L
    const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    val DOCUMENT_MIME_TYPES = arrayOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    )

    val DOCUMENT_MIME_SELECTION_PLACEHOLDERS = DOCUMENT_MIME_TYPES.joinToString { "?" }

    /**
     * Determines the FileType for a given file using MimeTypeMap.
     */
    fun getFileType(file: File): FileType {
        if (file.isDirectory) return FileType.DIRECTORY
        val mimeType = getMimeType(file) ?: return FileType.UNKNOWN
        return getFileTypeFromMimeType(mimeType)
    }

    fun getFileTypeFromName(name: String): FileType {
        val mimeType = getMimeTypeFromName(name) ?: return FileType.UNKNOWN
        return getFileTypeFromMimeType(mimeType)
    }

    fun getMimeTypeFromName(name: String): String? {
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun getFileTypeFromMimeType(mimeType: String): FileType {
        return when {
            mimeType.startsWith("image/") -> FileType.IMAGE
            mimeType.startsWith("video/") -> FileType.VIDEO
            mimeType.startsWith("audio/") -> FileType.AUDIO
            mimeType.startsWith("text/") -> FileType.DOCUMENT
            mimeType == APK_MIME_TYPE -> FileType.APK
            mimeType.contains("zip") || mimeType.contains("tar") ||
                mimeType.contains("rar") || mimeType.contains("compress") ||
                mimeType.contains("archive") -> FileType.ARCHIVE
            mimeType.contains("document") || mimeType.contains("pdf") ||
                mimeType.contains("word") || mimeType.contains("sheet") ||
                mimeType.contains("presentation") || mimeType.contains("json") ||
                mimeType.contains("xml") -> FileType.DOCUMENT
            else -> FileType.UNKNOWN
        }
    }

    /**
     * Gets mime type using Android's MimeTypeMap.
     */
    fun getMimeType(file: File): String? = getMimeTypeFromName(file.name)

    fun File.toFileItem(): FileItem {
        val fileType = getFileType(this)
        return FileItem(
            name = name,
            path = absolutePath,
            uri = toUri(),
            size = if (isFile) length() else 0,
            lastModified = lastModified(),
            isDirectory = isDirectory,
            fileType = fileType,
            mimeType = if (isFile) getMimeType(this) else null,
            childCount = if (isDirectory) listFiles()?.size else null,
        )
    }
}
