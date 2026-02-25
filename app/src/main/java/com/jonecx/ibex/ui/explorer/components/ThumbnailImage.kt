package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.ui.util.previewPlaceholder

@Composable
fun ThumbnailImage(
    fileItem: FileItem,
    modifier: Modifier = Modifier,
    onError: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val factory = LocalFileImageRequestFactory.current

    val imageRequest = remember(fileItem.path) { factory.create(context, fileItem) }

    AsyncImage(
        model = imageRequest,
        contentDescription = fileItem.name,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .previewPlaceholder(fileItem.fileType),
        contentScale = ContentScale.Crop,
        onState = { state ->
            if (state is AsyncImagePainter.State.Error) {
                onError?.invoke()
            }
        },
    )
}
