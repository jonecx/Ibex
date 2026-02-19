package com.jonecx.ibex.util

import android.webkit.MimeTypeMap
import com.jonecx.ibex.data.model.FileType
import java.io.File

object FileTypeUtils {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg")
    private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp")
    private val audioExtensions = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus")
    private val documentExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp", "csv", "json", "xml", "html", "md")
    private val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")

    fun getFileType(file: File): FileType {
        if (file.isDirectory) return FileType.DIRECTORY

        val extension = file.extension.lowercase()

        return when {
            extension in imageExtensions -> FileType.IMAGE
            extension in videoExtensions -> FileType.VIDEO
            extension in audioExtensions -> FileType.AUDIO
            extension in documentExtensions -> FileType.DOCUMENT
            extension in archiveExtensions -> FileType.ARCHIVE
            extension == "apk" -> FileType.APK
            else -> FileType.UNKNOWN
        }
    }

    fun getMimeType(file: File): String? {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun isMediaFile(fileType: FileType): Boolean {
        return fileType == FileType.IMAGE || fileType == FileType.VIDEO
    }
}
