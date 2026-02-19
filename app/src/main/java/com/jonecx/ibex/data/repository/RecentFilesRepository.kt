package com.jonecx.ibex.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.util.FileTypeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class RecentFilesRepository(
    private val context: Context,
    private val limit: Int = 20,
) : FileRepository {

    override fun getFiles(path: String): Flow<List<FileItem>> = flow {
        val recentFiles = queryRecentFiles()
        emit(recentFiles)
    }.flowOn(Dispatchers.IO)

    override fun getStorageRoots(): Flow<List<FileItem>> = flow {
        emit(emptyList())
    }

    override suspend fun getFileDetails(path: String): FileItem? {
        val file = File(path)
        return if (file.exists()) {
            FileItem(
                name = file.name,
                path = file.absolutePath,
                uri = file.toUri(),
                size = file.length(),
                lastModified = file.lastModified(),
                isDirectory = false,
                fileType = FileTypeUtils.getFileType(file),
                mimeType = FileTypeUtils.getMimeType(file),
            )
        } else {
            null
        }
    }

    private fun queryRecentFiles(): List<FileItem> {
        val recentFiles = mutableListOf<FileItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )

        // Exclude directories and trashed files
        val trashFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            " AND ${MediaStore.Files.FileColumns.IS_TRASHED} = 0"
        } else {
            ""
        }
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ?$trashFilter"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString())

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (cursor.moveToNext() && recentFiles.size < limit) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val data = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val date = cursor.getLong(dateColumn) * 1000
                val mime = cursor.getString(mimeColumn)
                val mediaType = cursor.getInt(mediaTypeColumn)

                val fileType = when (mediaType) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> FileType.IMAGE
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> FileType.VIDEO
                    MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> FileType.AUDIO
                    else -> {
                        if (data.isNotEmpty()) {
                            FileTypeUtils.getFileType(File(data))
                        } else {
                            FileType.UNKNOWN
                        }
                    }
                }

                val contentUri = ContentUris.withAppendedId(collection, id)

                recentFiles.add(
                    FileItem(
                        name = name,
                        path = data,
                        uri = contentUri,
                        size = size,
                        lastModified = date,
                        isDirectory = false,
                        fileType = fileType,
                        mimeType = mime,
                    ),
                )
            }
        }
        return recentFiles
    }
}
