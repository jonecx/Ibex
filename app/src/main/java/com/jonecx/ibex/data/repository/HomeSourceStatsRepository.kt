package com.jonecx.ibex.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.HomeStats
import com.jonecx.ibex.data.model.SourceStats
import com.jonecx.ibex.data.model.StorageUsage
import com.jonecx.ibex.util.MediaStoreUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

// Provides per-tile file count + total size for the home grid.
interface HomeSourceStatsRepository {
    suspend fun loadStats(): HomeStats
}

class MediaStoreHomeSourceStatsRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : HomeSourceStatsRepository {

    override suspend fun loadStats(): HomeStats = withContext(ioDispatcher) {
        val images = async { mediaStats(MediaStore.Images.Media.EXTERNAL_CONTENT_URI) }
        val videos = async { mediaStats(MediaStore.Video.Media.EXTERNAL_CONTENT_URI) }
        val audio = async { mediaStats(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) }
        val documents = async { MediaStoreUtils.queryDocumentStats(context) }
        val downloads = async { mediaStats(MediaStore.Downloads.EXTERNAL_CONTENT_URI) }
        val recent = async { recentStats() }
        val trash = async { trashStats() }
        val apps = async { appStats() }
        val storage = async { storageUsage() }

        HomeStats(
            sources = mapOf(
                FileSourceType.LOCAL_IMAGES to images.await(),
                FileSourceType.LOCAL_VIDEOS to videos.await(),
                FileSourceType.LOCAL_AUDIO to audio.await(),
                FileSourceType.LOCAL_DOCUMENTS to documents.await(),
                FileSourceType.LOCAL_DOWNLOADS to downloads.await(),
                FileSourceType.LOCAL_RECENT to recent.await(),
                FileSourceType.LOCAL_TRASH to trash.await(),
                FileSourceType.LOCAL_APPS to apps.await(),
            ),
            storageUsage = storage.await(),
        )
    }

    private fun mediaStats(collectionUri: Uri): SourceStats =
        MediaStoreUtils.queryStats(context, collectionUri, selection = MediaStoreUtils.trashFilter())

    // Files touched in the last week, mirroring the Recent screen's "non-media-none" filter.
    private fun recentStats(): SourceStats {
        val cutoffSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) -
            TimeUnit.DAYS.toSeconds(RECENT_WINDOW_DAYS)
        return MediaStoreUtils.queryStats(
            context = context,
            collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            selection = MediaStoreUtils.appendTrashFilter(
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} >= ? AND " +
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ?",
            ),
            selectionArgs = arrayOf(
                cutoffSeconds.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString(),
            ),
        )
    }

    // Trashed items are hidden from normal queries, so match them explicitly (R+ only).
    private fun trashStats(): SourceStats {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return SourceStats(count = 0, sizeBytes = 0L)
        val queryArgs = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Files.FileColumns.IS_TRASHED} = ?",
            )
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("1"))
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }
        return MediaStoreUtils.queryStats(
            context,
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            queryArgs,
        )
    }

    // User-installed apps only; size is the sum of their APK files.
    private fun appStats(): SourceStats {
        val userApps = context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
        val totalBytes = userApps.sumOf { File(it.sourceDir).length() }
        return SourceStats(count = userApps.size, sizeBytes = totalBytes)
    }

    private fun storageUsage(): StorageUsage {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val totalBytes = stat.totalBytes
        return StorageUsage(usedBytes = totalBytes - stat.availableBytes, totalBytes = totalBytes)
    }

    private companion object {
        const val RECENT_WINDOW_DAYS = 7L
    }
}
