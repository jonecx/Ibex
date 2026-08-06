package com.jonecx.ibex.ui.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.jonecx.ibex.ui.explorer.components.MediaViewerOverlay
import org.koin.androidx.compose.koinViewModel

@Composable
fun MediaViewerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaViewerViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.viewableFiles.isNotEmpty()) {
        MediaViewerOverlay(
            viewableFiles = uiState.viewableFiles,
            initialIndex = uiState.initialIndex,
            onDismiss = onNavigateBack,
            onDelete = { fileItem -> viewModel.deleteFile(fileItem) },
            modifier = modifier,
        )
    } else {
        LaunchedEffect(Unit) { onNavigateBack() }
    }
}
