package com.jonecx.ibex.util

import android.webkit.MimeTypeMap
import com.jonecx.ibex.data.model.FileType
import java.io.File

/**
 * Utility object for determining file types and mime types.
 * Uses Android's MimeTypeMap for mime type detection.
 */
object FileTypeUtils {

    /**
     * Determines the FileType for a given file using MimeTypeMap.
     */
    fun getFileType(file: File): FileType {
        if (file.isDirectory) return FileType.DIRECTORY
        val mimeType = getMimeType(file) ?: return FileType.UNKNOWN
        return getFileTypeFromMimeType(mimeType)
    }

    private fun getFileTypeFromMimeType(mimeType: String): FileType {
        return when {
            mimeType.startsWith("image/") -> FileType.IMAGE
            mimeType.startsWith("video/") -> FileType.VIDEO
            mimeType.startsWith("audio/") -> FileType.AUDIO
            mimeType.startsWith("text/") -> FileType.DOCUMENT
            mimeType == "application/vnd.android.package-archive" -> FileType.APK
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
    fun getMimeType(file: File): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
    }
}
