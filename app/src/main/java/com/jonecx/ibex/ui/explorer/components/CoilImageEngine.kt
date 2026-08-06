package com.jonecx.ibex.ui.explorer.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.jonecx.azmaree.image.AzmareeImageEngine
import com.jonecx.azmaree.image.model.AzmareeImageRequest
import com.jonecx.azmaree.image.model.AzmareeImageState

// Coil-backed engine for the Azmaree image viewer. Ibex builds fully-formed Coil ImageRequests via
// FileImageRequestFactory, so this engine passes request.data straight through and stays out of the
// way. It sets no explicit ImageLoader: AsyncImage uses the Coil singleton, so the app's
// ImageLoaderFactory and androidTest's Coil.setImageLoader overrides both keep working. Ibex renders
// placeholders through the request/modifier, not the loading/error slots, so those are unused here.
class CoilImageEngine : AzmareeImageEngine {

    @Composable
    override fun Image(
        request: AzmareeImageRequest,
        contentDescription: String?,
        modifier: Modifier,
        contentScale: ContentScale,
        alignment: Alignment,
        alpha: Float,
        colorFilter: ColorFilter?,
        onState: ((AzmareeImageState) -> Unit)?,
        loading: (@Composable () -> Unit)?,
        error: (@Composable () -> Unit)?,
    ) {
        AsyncImage(
            model = request.data,
            contentDescription = contentDescription,
            modifier = modifier,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            onState = onState?.let { cb ->
                {
                        state: AsyncImagePainter.State ->
                    val mapped = state.toAzmaree()
                    if (mapped != null) cb(mapped)
                }
            },
        )
    }
}

private fun AsyncImagePainter.State.toAzmaree(): AzmareeImageState? = when (this) {
    is AsyncImagePainter.State.Loading -> AzmareeImageState.Loading
    is AsyncImagePainter.State.Success -> AzmareeImageState.Success
    is AsyncImagePainter.State.Error -> AzmareeImageState.Error(result.throwable)
    AsyncImagePainter.State.Empty -> null
}
