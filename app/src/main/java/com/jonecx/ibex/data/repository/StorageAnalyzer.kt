package com.jonecx.ibex.data.repository

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import com.jonecx.ibex.data.model.StorageBreakdown
import com.jonecx.ibex.data.model.StorageCategory
import com.jonecx.ibex.di.IoDispatcher
import com.jonecx.ibex.ui.theme.SourceAppsColor
import com.jonecx.ibex.ui.theme.SourceAudioColor
import com.jonecx.ibex.ui.theme.SourceDocumentsColor
import com.jonecx.ibex.ui.theme.SourceImagesColor
import com.jonecx.ibex.ui.theme.SourceStorageColor
import com.jonecx.ibex.ui.theme.SourceVideosColor
import com.jonecx.ibex.util.FileTypeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface StorageAnalyzer {
    suspend fun analyze(): StorageBreakdown

    companion object {
        const val CATEGORY_IMAGES = "Images"
        const val CATEGORY_VIDEOS = "Videos"
        const val CATEGORY_AUDIO = "Audio"
        const val CATEGORY_DOCUMENTS = "Documents"
        const val CATEGORY_APPS = "Apps"
        const val CATEGORY_OTHER = "Other"
    }
}

@Singleton
class MediaStoreStorageAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : StorageAnalyzer {

    override suspend fun analyze(): StorageBreakdown = withContext(ioDispatcher) {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedBytes = totalBytes - freeBytes

        val imagesSizeDeferred = async { queryTotalSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI) }
        val videosSizeDeferred = async { queryTotalSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI) }
        val audioSizeDeferred = async { queryTotalSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) }
        val documentsSizeDeferred = async { queryDocumentsSize() }
        val appsSizeDeferred = async { queryAppsSize() }

        val imagesSize = imagesSizeDeferred.await()
        val videosSize = videosSizeDeferred.await()
        val audioSize = audioSizeDeferred.await()
        val documentsSize = documentsSizeDeferred.await()
        val appsSize = appsSizeDeferred.await()
        val otherSize = (usedBytes - imagesSize - videosSize - audioSize - documentsSize - appsSize)
            .coerceAtLeast(0)

        StorageBreakdown(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            categories = listOf(
                StorageCategory(StorageAnalyzer.CATEGORY_IMAGES, imagesSize, SourceImagesColor),
                StorageCategory(StorageAnalyzer.CATEGORY_VIDEOS, videosSize, SourceVideosColor),
                StorageCategory(StorageAnalyzer.CATEGORY_AUDIO, audioSize, SourceAudioColor),
                StorageCategory(StorageAnalyzer.CATEGORY_DOCUMENTS, documentsSize, SourceDocumentsColor),
                StorageCategory(StorageAnalyzer.CATEGORY_APPS, appsSize, SourceAppsColor),
                StorageCategory(StorageAnalyzer.CATEGORY_OTHER, otherSize, SourceStorageColor),
            ),
        )
    }

    private fun queryTotalSize(collectionUri: android.net.Uri): Long {
        var total = 0L
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "${MediaStore.MediaColumns.IS_TRASHED} = 0"
        } else {
            null
        }
        context.contentResolver.query(collectionUri, projection, selection, null, null)?.use { cursor ->
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                total += cursor.getLong(sizeColumn)
            }
        }
        return total
    }

    private fun queryDocumentsSize(): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0L
        var total = 0L
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(MediaStore.Files.FileColumns.SIZE)
        val trashFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            " AND ${MediaStore.Files.FileColumns.IS_TRASHED} = 0"
        } else {
            ""
        }
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (${FileTypeUtils.DOCUMENT_MIME_SELECTION_PLACEHOLDERS})$trashFilter"
        val selectionArgs = FileTypeUtils.DOCUMENT_MIME_TYPES
        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            while (cursor.moveToNext()) {
                total += cursor.getLong(sizeColumn)
            }
        }
        return total
    }

    private fun queryAppsSize(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val uuid = storageManager.primaryStorageVolume.uuid
                    ?.let { UUID.fromString(it) }
                    ?: StorageManager.UUID_DEFAULT
                val stats = storageStatsManager.queryStatsForUid(uuid, android.os.Process.myUid())
                stats.appBytes + stats.cacheBytes
            } catch (_: Exception) {
                0L
            }
        } else {
            0L
        }
    }
}
