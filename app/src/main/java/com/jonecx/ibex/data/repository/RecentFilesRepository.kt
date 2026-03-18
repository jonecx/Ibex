package com.jonecx.ibex.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.util.FileTypeUtils
import com.jonecx.ibex.util.MediaStoreUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class RecentFilesRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
    private val limit: Int = 20,
) : FileRepository {

    override fun getFiles(path: String): Flow<List<FileItem>> = flow {
        val recentFiles = queryRecentFiles()
        emit(recentFiles)
    }.flowOn(ioDispatcher)

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
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )

        val selection = MediaStoreUtils.appendTrashFilter(
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ?",
        )
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString())

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (cursor.moveToNext() && recentFiles.size < limit) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: context.getString(R.string.unknown_file)
                val data = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val date = cursor.getLong(dateColumn) * FileTypeUtils.SECONDS_TO_MILLIS
                val dateAdded = cursor.getLong(dateAddedColumn) * FileTypeUtils.SECONDS_TO_MILLIS
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
                        createdAt = dateAdded,
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
