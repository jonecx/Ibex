package com.jonecx.ibex.util

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import com.jonecx.ibex.R

/**
 * A fake [ImageLoader] that returns [R.drawable.sample_image_thumbnail] for every
 * request, causing Coil to report [SuccessResult] without loading real files.
 *
 * This ensures [ThumbnailImage] receives a success state in tests, so
 * [FileGridItem] hides filename text for image/video types (matching production
 * behavior when thumbnails load successfully).
 *
 * Usage: `Coil.setImageLoader(FakeImageLoader(context))` in test setup.
 */
class FakeImageLoader(private val context: Context) : ImageLoader {

    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? get() = null
    override val diskCache: DiskCache? get() = null

    private fun placeholder(): Drawable =
        ContextCompat.getDrawable(context, R.drawable.sample_image_thumbnail)!!

    override fun enqueue(request: ImageRequest): Disposable {
        request.target?.onStart(request.placeholder)
        val drawable = placeholder()
        request.target?.onSuccess(drawable)
        return object : Disposable {
            override val job = kotlinx.coroutines.CompletableDeferred(
                SuccessResult(
                    drawable = drawable,
                    request = request,
                    dataSource = DataSource.MEMORY,
                ),
            )
            override val isDisposed get() = true
            override fun dispose() {}
        }
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        return SuccessResult(
            drawable = placeholder(),
            request = request,
            dataSource = DataSource.MEMORY,
        )
    }

    override fun newBuilder(): ImageLoader.Builder {
        throw UnsupportedOperationException()
    }

    override fun shutdown() {}
}
