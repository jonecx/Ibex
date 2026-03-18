package com.jonecx.ibex.data.repository

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.util.MediaStoreUtils
import com.jonecx.ibex.util.MediaStoreUtils.toFileItems
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

    override suspend fun getFileDetails(path: String): FileItem? = null

    private fun queryTrashedFiles(): List<FileItem> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()

        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val selection = "${MediaStore.Files.FileColumns.IS_TRASHED} = ?"
        val selectionArgs = arrayOf("1")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        val queryArgs = android.os.Bundle().apply {
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }

        return context.contentResolver.query(collection, MediaStoreUtils.PROJECTION, queryArgs, null)
            ?.use { cursor -> cursor.toFileItems(collection, context) }
            ?: emptyList()
    }
}
