package com.jonecx.ibex.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileType

@Composable
fun Modifier.previewPlaceholder(fileType: FileType): Modifier {
    if (!LocalInspectionMode.current) return this
    val resId = when (fileType) {
        FileType.VIDEO -> R.drawable.sample_video_thumbnail
        else -> R.drawable.sample_image_thumbnail
    }
    val painter = painterResource(resId)
    return this.then(
        Modifier.drawWithContent {
            with(painter) {
                draw(size)
            }
        },
    )
}
