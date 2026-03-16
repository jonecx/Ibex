package com.jonecx.ibex

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.jonecx.ibex.fixtures.FakeStorageAnalyzer
import com.jonecx.ibex.ui.analysis.StorageAnalysisScreenContent
import com.jonecx.ibex.ui.analysis.StorageAnalysisUiState
import com.jonecx.ibex.ui.theme.IbexTheme

private val sampleBreakdown = kotlinx.coroutines.runBlocking {
    FakeStorageAnalyzer().analyze()
}

@Composable
private fun StorageAnalysisPreview(
    uiState: StorageAnalysisUiState = StorageAnalysisUiState(
        isLoading = false,
        breakdown = sampleBreakdown,
    ),
) {
    StorageAnalysisScreenContent(
        uiState = uiState,
        onNavigateBack = {},
        onRetry = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun StorageAnalysisScreenPreview() {
    IbexTheme {
        StorageAnalysisPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun StorageAnalysisScreenLoadingPreview() {
    IbexTheme {
        StorageAnalysisPreview(uiState = StorageAnalysisUiState(isLoading = true))
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun StorageAnalysisScreenDarkThemePreview() {
    IbexTheme(darkTheme = true) {
        StorageAnalysisPreview()
    }
}
