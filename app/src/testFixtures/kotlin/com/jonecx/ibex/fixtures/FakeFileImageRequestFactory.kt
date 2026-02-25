package com.jonecx.ibex.fixtures

import android.content.Context
import coil.request.ImageRequest
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.ui.explorer.components.FileImageRequestFactory

class FakeFileImageRequestFactory : FileImageRequestFactory {
    override fun create(context: Context, fileItem: FileItem): ImageRequest {
        return ImageRequest.Builder(context)
            .data(fileItem.path)
            .build()
    }
}
