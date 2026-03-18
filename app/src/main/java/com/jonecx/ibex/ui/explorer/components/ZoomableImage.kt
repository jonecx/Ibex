package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.jonecx.ibex.data.model.FileItem

@Composable
fun ZoomableImage(
    fileItem: FileItem,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
) {
    val context = LocalContext.current
    val factory = LocalFileImageRequestFactory.current
    var scale by remember { mutableFloatStateOf(MIN_ZOOM) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val currentOnTap by rememberUpdatedState(onTap)

    val imageRequest = remember(fileItem.path) { factory.create(context, fileItem, fullSize = true) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = fileItem.name,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.size >= 2) {
                                val zoom = event.calculateZoom()
                                if (zoom != MIN_ZOOM) {
                                    scale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                    event.changes.forEach { it.consume() }
                                }
                                if (scale > MIN_ZOOM) {
                                    val pan = event.calculatePan()
                                    offsetX += pan.x
                                    offsetY += pan.y
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { currentOnTap() },
                        onDoubleTap = {
                            if (scale > MIN_ZOOM) {
                                scale = MIN_ZOOM
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = DOUBLE_TAP_ZOOM
                            }
                        },
                    )
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                ),
            contentScale = ContentScale.Fit,
        )
    }
}

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
