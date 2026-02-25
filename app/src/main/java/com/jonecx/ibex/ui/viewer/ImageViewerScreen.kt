package com.jonecx.ibex.ui.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.jonecx.ibex.ui.explorer.components.ImageViewerOverlay

@Composable
fun ImageViewerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.viewableFiles.isNotEmpty()) {
        ImageViewerOverlay(
            viewableFiles = uiState.viewableFiles,
            initialIndex = uiState.initialIndex,
            onDismiss = onNavigateBack,
            modifier = modifier,
        )
    }
}
