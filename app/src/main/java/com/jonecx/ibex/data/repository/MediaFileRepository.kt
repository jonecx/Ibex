package com.jonecx.ibex.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.util.FileTypeUtils
import com.jonecx.ibex.util.MediaStoreUtils
import com.jonecx.ibex.util.MediaStoreUtils.toFileItems
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

enum class MediaType {
    IMAGES, VIDEOS, AUDIO, DOCUMENTS
}

class MediaFileRepository(
    private val context: Context,
    private val mediaType: MediaType,
    private val ioDispatcher: CoroutineDispatcher,
) : FileRepository {

    override fun getFiles(path: String): Flow<List<FileItem>> = flow {
        val files = when (mediaType) {
            MediaType.IMAGES -> queryImages()
            MediaType.VIDEOS -> queryVideos()
            MediaType.AUDIO -> queryAudio()
            MediaType.DOCUMENTS -> queryDocuments()
        }
        emit(files)
    }.flowOn(ioDispatcher)

    private fun queryImages() = queryMediaStore(
        collection = getCollectionUri(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        ),
        fileType = FileType.IMAGE,
    )

    private fun queryVideos() = queryMediaStore(
        collection = getCollectionUri(
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ),
        fileType = FileType.VIDEO,
    )

    private fun queryAudio() = queryMediaStore(
        collection = getCollectionUri(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        ),
        fileType = FileType.AUDIO,
    )

    private fun queryDocuments(): List<FileItem> {
        val collection: Uri
        val selection: String
        val selectionArgs: Array<String>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            selection = MediaStoreUtils.appendTrashFilter(
                "${MediaStore.Files.FileColumns.MIME_TYPE} IN (${FileTypeUtils.DOCUMENT_MIME_SELECTION_PLACEHOLDERS})",
            )
            selectionArgs = FileTypeUtils.DOCUMENT_MIME_TYPES
        } else {
            collection = MediaStore.Files.getContentUri("external")
            selection = LEGACY_DOCUMENT_EXTENSIONS.joinToString(" OR ") {
                "${MediaStore.Files.FileColumns.DATA} LIKE ?"
            }
            selectionArgs = LEGACY_DOCUMENT_EXTENSIONS
        }

        return queryMediaStore(
            collection = collection,
            fileType = FileType.DOCUMENT,
            selection = selection,
            selectionArgs = selectionArgs,
        )
    }

    private fun queryMediaStore(
        collection: Uri,
        fileType: FileType,
        selection: String? = MediaStoreUtils.trashFilter(),
        selectionArgs: Array<String>? = null,
    ): List<FileItem> {
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        return context.contentResolver.query(collection, MediaStoreUtils.PROJECTION, selection, selectionArgs, sortOrder)
            ?.use { cursor -> cursor.toFileItems(collection, context, fileType) }
            ?: emptyList()
    }

    private fun getCollectionUri(qUri: Uri, fallbackUri: Uri): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) qUri else fallbackUri

    companion object {
        private val LEGACY_DOCUMENT_EXTENSIONS = arrayOf("%.pdf", "%.doc", "%.docx", "%.xls", "%.xlsx")
    }
}
