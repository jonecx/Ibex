package com.jonecx.ibex.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
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

// Browses the real folder tree, but only surfaces folders that contain media of [mediaType] plus the
// media files themselves. Each folder tile carries a cover (its newest item) and a recursive count, so
// the Images/Videos tabs group into albums the way a gallery does while still nesting into subfolders.
class MediaFolderRepository(
    private val context: Context,
    private val mediaType: MediaType,
    private val ioDispatcher: CoroutineDispatcher,
) : FileRepository {

    private val isVideo: Boolean = mediaType == MediaType.VIDEOS

    override fun getFiles(path: String): Flow<List<FileItem>> = flow {
        emit(listFolder(path))
    }.flowOn(ioDispatcher)

    private fun listFolder(path: String): List<FileItem> {
        val prefix = "${path.trimEnd('/')}/"
        val collection = collectionUri()

        val selection = buildString {
            append(MediaStore.MediaColumns.DATA).append(" LIKE ? ESCAPE '\\'")
            MediaStoreUtils.trashFilter()?.let { append(" AND ").append(it) }
        }
        val selectionArgs = arrayOf("${prefix.escapeLike()}%")
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        return context.contentResolver
            .query(collection, MediaStoreUtils.PROJECTION, selection, selectionArgs, sortOrder)
            ?.use { it.toMediaFolderListing(prefix, isVideo, collection, context.getString(R.string.unknown_file)) }
            ?: emptyList()
    }

    private fun collectionUri(): Uri {
        val volumeUri = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }
        val legacyUri = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) volumeUri else legacyUri
    }
}

// Folds a media cursor (sorted newest-first) into immediate subfolders and loose files under [prefix]. Each
// folder gets a recursive count and the newest item beneath it as its cover. Kept separate so it can be
// unit-tested with a plain cursor. MediaStore paths always use '/', so the host separator is never involved.
internal fun Cursor.toMediaFolderListing(
    prefix: String,
    isVideo: Boolean,
    collection: Uri,
    unknownName: String,
): List<FileItem> {
    val fileType = if (isVideo) FileType.VIDEO else FileType.IMAGE
    val idCol = getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
    val nameCol = getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
    val dataCol = getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
    val sizeCol = getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
    val dateCol = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
    val dateAddedCol = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
    val mimeCol = getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

    val directFiles = mutableListOf<FileItem>()
    // Insertion order tracks recency: the first row for a subfolder (newest, by sort) becomes its cover.
    val folders = LinkedHashMap<String, FolderAccumulator>()

    while (moveToNext()) {
        val data = getString(dataCol) ?: continue
        if (!data.startsWith(prefix)) continue
        val relative = data.substring(prefix.length)
        val separator = relative.indexOf('/')
        val modifiedMs = getLong(dateCol) * FileTypeUtils.SECONDS_TO_MILLIS
        if (separator < 0) {
            directFiles.add(
                FileItem(
                    name = getString(nameCol) ?: unknownName,
                    path = data,
                    uri = ContentUris.withAppendedId(collection, getLong(idCol)),
                    size = getLong(sizeCol),
                    lastModified = modifiedMs,
                    createdAt = getLong(dateAddedCol) * FileTypeUtils.SECONDS_TO_MILLIS,
                    isDirectory = false,
                    fileType = fileType,
                    mimeType = getString(mimeCol),
                ),
            )
        } else {
            val folderName = relative.substring(0, separator)
            folders.getOrPut(folderName) { FolderAccumulator(coverPath = data, newestModified = modifiedMs) }.count++
        }
    }

    val parent = prefix.trimEnd('/')
    val folderItems = folders.map { (folderName, acc) ->
        val folderPath = "$parent/$folderName"
        FileItem(
            name = folderName,
            path = folderPath,
            uri = Uri.fromFile(File(folderPath)),
            size = 0L,
            lastModified = acc.newestModified,
            isDirectory = true,
            fileType = FileType.DIRECTORY,
            childCount = acc.count,
            coverPath = acc.coverPath,
            coverIsVideo = isVideo,
        )
    }
    return folderItems + directFiles
}

private class FolderAccumulator(val coverPath: String, val newestModified: Long) {
    var count: Int = 0
}

// Escapes LIKE wildcards so a folder name containing % or _ can't widen the match. Pairs with ESCAPE '\'.
private fun String.escapeLike(): String =
    replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
