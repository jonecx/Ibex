package com.jonecx.ibex

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.jonecx.ibex.ui.components.FastScrollOverlay
import com.jonecx.ibex.ui.theme.IbexTheme

@Composable
private fun FastScrollerFrame(
    dragging: Boolean,
    label: String,
) {
    Box(
        modifier = Modifier
            .height(360.dp)
            .width(140.dp),
    ) {
        FastScrollOverlay(
            alpha = 1f,
            thumbFraction = 0.4f,
            dragging = dragging,
            bubbleFraction = 0.4f,
            label = label,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun FastScrollerRestingPreview() {
    IbexTheme {
        FastScrollerFrame(dragging = false, label = "F")
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun FastScrollerDraggingPreview() {
    IbexTheme {
        FastScrollerFrame(dragging = true, label = "F")
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun FastScrollerDraggingDarkPreview() {
    IbexTheme(darkTheme = true) {
        FastScrollerFrame(dragging = true, label = "F")
    }
}
