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

enum class MediaType {
    IMAGES, VIDEOS, AUDIO, DOCUMENTS
}

class MediaFileRepository(
    private val context: Context,
    private val mediaType: MediaType,
) : FileRepository {

    override fun getFiles(path: String): Flow<List<FileItem>> = flow {
        val files = when (mediaType) {
            MediaType.IMAGES -> queryImages()
            MediaType.VIDEOS -> queryVideos()
            MediaType.AUDIO -> queryAudio()
            MediaType.DOCUMENTS -> queryDocuments()
        }
        emit(files)
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

    private fun queryImages(): List<FileItem> {
        val images = mutableListOf<FileItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE,
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "${MediaStore.Images.Media.IS_TRASHED} = 0"
        } else {
            null
        }

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val data = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val date = cursor.getLong(dateColumn) * 1000
                val mime = cursor.getString(mimeColumn)
                val contentUri = ContentUris.withAppendedId(collection, id)

                images.add(
                    FileItem(
                        name = name,
                        path = data,
                        uri = contentUri,
                        size = size,
                        lastModified = date,
                        isDirectory = false,
                        fileType = FileType.IMAGE,
                        mimeType = mime,
                    ),
                )
            }
        }
        return images
    }

    private fun queryVideos(): List<FileItem> {
        val videos = mutableListOf<FileItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "${MediaStore.Video.Media.IS_TRASHED} = 0"
        } else {
            null
        }

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val data = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val date = cursor.getLong(dateColumn) * 1000
                val mime = cursor.getString(mimeColumn)
                val contentUri = ContentUris.withAppendedId(collection, id)

                videos.add(
                    FileItem(
                        name = name,
                        path = data,
                        uri = contentUri,
                        size = size,
                        lastModified = date,
                        isDirectory = false,
                        fileType = FileType.VIDEO,
                        mimeType = mime,
                    ),
                )
            }
        }
        return videos
    }

    private fun queryAudio(): List<FileItem> {
        val audioFiles = mutableListOf<FileItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE,
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "${MediaStore.Audio.Media.IS_TRASHED} = 0"
        } else {
            null
        }

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val data = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val date = cursor.getLong(dateColumn) * 1000
                val mime = cursor.getString(mimeColumn)
                val contentUri = ContentUris.withAppendedId(collection, id)

                audioFiles.add(
                    FileItem(
                        name = name,
                        path = data,
                        uri = contentUri,
                        size = size,
                        lastModified = date,
                        isDirectory = false,
                        fileType = FileType.AUDIO,
                        mimeType = mime,
                    ),
                )
            }
        }
        return audioFiles
    }

    private fun queryDocuments(): List<FileItem> {
        val documents = mutableListOf<FileItem>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE,
            )

            val trashFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                " AND ${MediaStore.Files.FileColumns.IS_TRASHED} = 0"
            } else {
                ""
            }
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?, ?, ?, ?, ?)$trashFilter"
            val selectionArgs = arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            )
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val data = cursor.getString(dataColumn) ?: ""
                    val size = cursor.getLong(sizeColumn)
                    val date = cursor.getLong(dateColumn) * 1000
                    val mime = cursor.getString(mimeColumn)
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    documents.add(
                        FileItem(
                            name = name,
                            path = data,
                            uri = contentUri,
                            size = size,
                            lastModified = date,
                            isDirectory = false,
                            fileType = FileType.DOCUMENT,
                            mimeType = mime,
                        ),
                    )
                }
            }
        } else {
            // For older versions, scan common document extensions
            val collection = MediaStore.Files.getContentUri("external")

            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE,
            )

            val selection = "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE ?"
            val selectionArgs = arrayOf("%.pdf", "%.doc", "%.docx", "%.xls", "%.xlsx")
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val data = cursor.getString(dataColumn) ?: ""
                    val size = cursor.getLong(sizeColumn)
                    val date = cursor.getLong(dateColumn) * 1000
                    val mime = cursor.getString(mimeColumn)
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    documents.add(
                        FileItem(
                            name = name,
                            path = data,
                            uri = contentUri,
                            size = size,
                            lastModified = date,
                            isDirectory = false,
                            fileType = FileType.DOCUMENT,
                            mimeType = mime,
                        ),
                    )
                }
            }
        }
        return documents
    }
}
