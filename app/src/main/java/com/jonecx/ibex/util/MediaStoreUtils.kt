package com.jonecx.ibex.util

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType

object MediaStoreUtils {

    val PROJECTION = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.DATA,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.MIME_TYPE,
    )

    fun trashFilter(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "${MediaStore.MediaColumns.IS_TRASHED} = 0"
        } else {
            null
        }

    fun appendTrashFilter(base: String): String {
        val filter = trashFilter() ?: return base
        return "$base AND $filter"
    }

    fun resolveFileType(mimeType: String?, fileName: String): FileType {
        if (mimeType != null) {
            val type = FileTypeUtils.getFileTypeFromMimeType(mimeType)
            if (type != FileType.UNKNOWN) return type
        }
        return FileTypeUtils.getFileTypeFromName(fileName)
    }

    fun sumColumnSize(
        context: Context,
        collection: Uri,
        sizeColumn: String = MediaStore.MediaColumns.SIZE,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
    ): Long {
        var total = 0L
        context.contentResolver.query(
            collection,
            arrayOf(sizeColumn),
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val colIndex = cursor.getColumnIndexOrThrow(sizeColumn)
            while (cursor.moveToNext()) {
                total += cursor.getLong(colIndex)
            }
        }
        return total
    }

    fun Cursor.toFileItems(
        collection: Uri,
        context: Context,
        fileType: FileType? = null,
        limit: Int = Int.MAX_VALUE,
    ): List<FileItem> {
        val items = mutableListOf<FileItem>()
        val idCol = getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val nameCol = getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val dataCol = getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        val sizeCol = getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val dateCol = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
        val mimeCol = getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

        while (moveToNext() && items.size < limit) {
            val id = getLong(idCol)
            val name = getString(nameCol) ?: context.getString(R.string.unknown_file)
            val path = getString(dataCol) ?: ""
            val mimeType = getString(mimeCol)
            items.add(
                FileItem(
                    name = name,
                    path = path,
                    uri = ContentUris.withAppendedId(collection, id),
                    size = getLong(sizeCol),
                    lastModified = getLong(dateCol) * FileTypeUtils.SECONDS_TO_MILLIS,
                    isDirectory = false,
                    fileType = fileType ?: resolveFileType(mimeType, name),
                    mimeType = mimeType,
                ),
            )
        }
        return items
    }
}
