package com.jonecx.ibex.di

import android.content.Context
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import com.jonecx.ibex.data.repository.SmbContextProviderContract
import com.jonecx.ibex.ui.explorer.components.SmbFetcherFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        smbContextProvider: SmbContextProviderContract,
    ): ImageLoader {
        val smbCacheDir = File(context.cacheDir, SMB_THUMBNAIL_CACHE_DIR)
        smbCacheDir.mkdirs()
        return ImageLoader.Builder(context)
            .components {
                add(SmbFetcherFactory(smbContextProvider, smbCacheDir))
                add(VideoFrameDecoder.Factory())
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, COIL_DISK_CACHE_DIR))
                    .maxSizeBytes(DISK_CACHE_MAX_SIZE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    private const val SMB_THUMBNAIL_CACHE_DIR = "smb_thumbnails"
    private const val COIL_DISK_CACHE_DIR = "coil_cache"
    private const val DISK_CACHE_MAX_SIZE_BYTES = 50L * 1024 * 1024
}
