package com.jonecx.ibex.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface FileTrashManager {
    suspend fun trashFile(fileItem: FileItem): Boolean
}

@Singleton
class MediaStoreFileTrashManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FileTrashManager {

    override suspend fun trashFile(fileItem: FileItem): Boolean = withContext(ioDispatcher) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@withContext deleteFileDirect(fileItem)
        }

        val contentUri = findContentUri(fileItem.path)
        if (contentUri != null) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_TRASHED, 1)
            }
            val updated = context.contentResolver.update(contentUri, values, null, null)
            updated > 0
        } else {
            deleteFileDirect(fileItem)
        }
    }

    private fun findContentUri(path: String): android.net.Uri? {
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
        val selectionArgs = arrayOf(path)

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                return android.content.ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }

    private fun deleteFileDirect(fileItem: FileItem): Boolean {
        val file = java.io.File(fileItem.path)
        return file.exists() && file.delete()
    }
}
