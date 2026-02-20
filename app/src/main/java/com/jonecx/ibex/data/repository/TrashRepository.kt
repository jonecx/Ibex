package com.jonecx.ibex.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class TrashRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : FileRepository {

    override fun getFiles(path: String): Flow<List<FileItem>> = flow {
        val trashedFiles = mutableListOf<FileItem>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            trashedFiles.addAll(queryTrashedFiles())
        }

        emit(trashedFiles.sortedByDescending { it.lastModified })
    }.flowOn(ioDispatcher)

    override fun getStorageRoots(): Flow<List<FileItem>> = flow {
        emit(emptyList())
    }

    override suspend fun getFileDetails(path: String): FileItem? = null

    private fun queryTrashedFiles(): List<FileItem> {
        val trashedFiles = mutableListOf<FileItem>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return trashedFiles
        }

        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA,
        )

        val selection = "${MediaStore.Files.FileColumns.IS_TRASHED} = ?"
        val selectionArgs = arrayOf("1")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        val queryArgs = android.os.Bundle().apply {
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }

        context.contentResolver.query(
            collection,
            projection,
            queryArgs,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: context.getString(R.string.unknown_file)
                val size = cursor.getLong(sizeColumn)
                val dateModified = cursor.getLong(dateColumn) * 1000
                val mimeType = cursor.getString(mimeColumn)
                val path = cursor.getString(dataColumn) ?: ""

                val uri = ContentUris.withAppendedId(collection, id)
                val fileType = getFileType(mimeType, name)

                trashedFiles.add(
                    FileItem(
                        name = name,
                        path = path,
                        size = size,
                        lastModified = dateModified,
                        isDirectory = false,
                        fileType = fileType,
                        mimeType = mimeType,
                        uri = uri,
                    ),
                )
            }
        }

        return trashedFiles
    }

    private fun getFileType(mimeType: String?, fileName: String): FileType {
        return when {
            mimeType?.startsWith("image/") == true -> FileType.IMAGE
            mimeType?.startsWith("video/") == true -> FileType.VIDEO
            mimeType?.startsWith("audio/") == true -> FileType.AUDIO
            mimeType?.startsWith("application/pdf") == true -> FileType.DOCUMENT
            mimeType?.startsWith("application/msword") == true -> FileType.DOCUMENT
            mimeType?.startsWith("application/vnd.ms") == true -> FileType.DOCUMENT
            mimeType?.startsWith("application/vnd.openxmlformats") == true -> FileType.DOCUMENT
            mimeType?.startsWith("text/") == true -> FileType.DOCUMENT
            fileName.endsWith(".apk", ignoreCase = true) -> FileType.APK
            else -> FileType.UNKNOWN
        }
    }
}
