package com.jonecx.ibex.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.jonecx.ibex.data.model.StorageBreakdown
import com.jonecx.ibex.data.model.StorageCategory
import com.jonecx.ibex.ui.theme.SourceAppsColor
import com.jonecx.ibex.ui.theme.SourceAudioColor
import com.jonecx.ibex.ui.theme.SourceDocumentsColor
import com.jonecx.ibex.ui.theme.SourceImagesColor
import com.jonecx.ibex.ui.theme.SourceStorageColor
import com.jonecx.ibex.ui.theme.SourceVideosColor
import com.jonecx.ibex.util.AppStorageUtils
import com.jonecx.ibex.util.MediaStoreUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

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

class MediaStoreStorageAnalyzer(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
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

    private fun queryTotalSize(collectionUri: android.net.Uri): Long =
        MediaStoreUtils.sumColumnSize(context, collectionUri, selection = MediaStoreUtils.trashFilter())

    private fun queryDocumentsSize(): Long = MediaStoreUtils.queryDocumentStats(context).sizeBytes

    // Sum of every user-installed app's APK, matching the home "Apps" tile.
    private fun queryAppsSize(): Long = AppStorageUtils.appStats(context).sizeBytes
}
