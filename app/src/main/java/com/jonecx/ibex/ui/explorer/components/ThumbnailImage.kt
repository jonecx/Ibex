package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
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

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = fileItem.name,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .previewPlaceholder(fileItem.fileType),
            contentScale = ContentScale.Crop,
            onState = { state ->
                if (state is AsyncImagePainter.State.Error) {
                    onError?.invoke()
                }
            },
        )
        if (fileItem.fileType == FileType.VIDEO) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
