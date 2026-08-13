package com.jonecx.ibex.util

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.data.model.SourceStats

object MediaStoreUtils {

    val PROJECTION = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.DATA,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.DATE_ADDED,
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
    ): Long = queryStats(context, collection, sizeColumn, selection, selectionArgs).sizeBytes

    // Sums the size column and counts matching rows in a single cursor pass.
    fun queryStats(
        context: Context,
        collection: Uri,
        sizeColumn: String = MediaStore.MediaColumns.SIZE,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
    ): SourceStats =
        context.contentResolver.query(collection, arrayOf(sizeColumn), selection, selectionArgs, null)
            .foldStats(sizeColumn)

    // Bundle variant for queries needing QUERY_ARG_* extras (e.g. matching trashed items).
    fun queryStats(
        context: Context,
        collection: Uri,
        queryArgs: Bundle,
        sizeColumn: String = MediaStore.MediaColumns.SIZE,
    ): SourceStats =
        context.contentResolver.query(collection, arrayOf(sizeColumn), queryArgs, null)
            .foldStats(sizeColumn)

    private fun Cursor?.foldStats(sizeColumn: String): SourceStats {
        var total = 0L
        var count = 0
        this?.use { cursor ->
            val colIndex = cursor.getColumnIndexOrThrow(sizeColumn)
            while (cursor.moveToNext()) {
                total += cursor.getLong(colIndex)
                count++
            }
        }
        return SourceStats(count = count, sizeBytes = total)
    }

    // Documents live in MediaStore.Files, filtered to the known office/pdf mime types.
    fun queryDocumentStats(context: Context): SourceStats {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return SourceStats(count = 0, sizeBytes = 0L)
        return queryStats(
            context = context,
            collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            selection = appendTrashFilter(
                "${MediaStore.Files.FileColumns.MIME_TYPE} IN (${FileTypeUtils.DOCUMENT_MIME_SELECTION_PLACEHOLDERS})",
            ),
            selectionArgs = FileTypeUtils.DOCUMENT_MIME_TYPES,
        )
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
        val dateAddedCol = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
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
                    createdAt = getLong(dateAddedCol) * FileTypeUtils.SECONDS_TO_MILLIS,
                    isDirectory = false,
                    fileType = fileType ?: resolveFileType(mimeType, name),
                    mimeType = mimeType,
                ),
            )
        }
        return items
    }
}
