package com.jonecx.ibex.di

import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import com.jonecx.ibex.data.repository.SmbContextProviderContract
import com.jonecx.ibex.ui.explorer.components.SmbFetcherFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

private const val SMB_THUMBNAIL_CACHE_DIR = "smb_thumbnails"
private const val COIL_DISK_CACHE_DIR = "coil_cache"
private const val DISK_CACHE_MAX_SIZE_BYTES = 50L * 1024 * 1024

val imageLoaderModule = module {
    single<ImageLoader> {
        val context = androidContext()
        val smbContextProvider = get<SmbContextProviderContract>()
        val smbCacheDir = File(context.cacheDir, SMB_THUMBNAIL_CACHE_DIR)
        smbCacheDir.mkdirs()
        ImageLoader.Builder(context)
            .components {
                add(SmbFetcherFactory(smbContextProvider, smbCacheDir))
                add(ImageDecoderDecoder.Factory())
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
}
