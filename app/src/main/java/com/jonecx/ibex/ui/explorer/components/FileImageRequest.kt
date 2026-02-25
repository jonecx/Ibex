package com.jonecx.ibex.ui.explorer.components

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import javax.inject.Inject

interface FileImageRequestFactory {
    fun create(context: Context, fileItem: FileItem): ImageRequest
}

class DefaultFileImageRequestFactory @Inject constructor() : FileImageRequestFactory {
    override fun create(context: Context, fileItem: FileItem): ImageRequest {
        return ImageRequest.Builder(context)
            .data(fileItem.path)
            .apply {
                if (fileItem.fileType == FileType.VIDEO) {
                    videoFrameMillis(1000)
                }
            }
            .build()
    }
}

val LocalFileImageRequestFactory = staticCompositionLocalOf<FileImageRequestFactory> {
    error("No FileImageRequestFactory provided")
}
