package com.jonecx.ibex.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    description: String? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val indicatorModifier = if (description != null) {
            Modifier.semantics { contentDescription = description }
        } else {
            Modifier
        }
        if (color != Color.Unspecified) {
            CircularProgressIndicator(modifier = indicatorModifier, color = color)
        } else {
            CircularProgressIndicator(modifier = indicatorModifier)
        }
    }
}
