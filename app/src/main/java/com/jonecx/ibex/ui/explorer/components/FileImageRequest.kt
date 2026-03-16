package com.jonecx.ibex.ui.explorer.components

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import coil.request.ImageRequest
import coil.request.Parameters
import coil.request.videoFrameMillis
import coil.size.Size
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import javax.inject.Inject

interface FileImageRequestFactory {
    fun create(context: Context, fileItem: FileItem, fullSize: Boolean = false): ImageRequest
}

class DefaultFileImageRequestFactory @Inject constructor() : FileImageRequestFactory {
    override fun create(context: Context, fileItem: FileItem, fullSize: Boolean): ImageRequest {
        return ImageRequest.Builder(context)
            .data(fileItem.path)
            .size(if (fullSize) Size.ORIGINAL else Size(THUMBNAIL_SIZE_PX, THUMBNAIL_SIZE_PX))
            .crossfade(true)
            .apply {
                if (fullSize) {
                    parameters(Parameters.Builder().set(FULL_SIZE_KEY, FULL_SIZE_VALUE).build())
                }
                if (fileItem.fileType == FileType.VIDEO) {
                    videoFrameMillis(1000)
                }
            }
            .build()
    }

    companion object {
        const val FULL_SIZE_KEY = "ibex_full_size"
        const val FULL_SIZE_VALUE = "true"
        private const val THUMBNAIL_SIZE_PX = 256
    }
}

val LocalFileImageRequestFactory = staticCompositionLocalOf<FileImageRequestFactory> {
    error("No FileImageRequestFactory provided")
}
