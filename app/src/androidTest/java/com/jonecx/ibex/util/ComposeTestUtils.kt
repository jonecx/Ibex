package com.jonecx.ibex.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.ui.settings.SettingsScreenContent
import com.jonecx.ibex.ui.settings.SettingsUiState
import com.jonecx.ibex.ui.theme.IbexTheme

fun AndroidComposeTestRule<*, *>.runOnUiThreadBlocking(block: suspend () -> Unit) {
    runOnUiThread {
        kotlinx.coroutines.runBlocking { block() }
    }
}

fun ComposeContentTestRule.setIbexContent(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    setContent {
        IbexTheme(darkTheme = darkTheme) {
            content()
        }
    }
}

fun ComposeContentTestRule.setSettingsContent(
    uiState: SettingsUiState = SettingsUiState(),
    onNavigateBack: () -> Unit = {},
    onAnalyticsToggleChanged: (Boolean) -> Unit = {},
    onViewModeChanged: (ViewMode) -> Unit = {},
) {
    setIbexContent {
        SettingsScreenContent(
            uiState = uiState,
            onNavigateBack = onNavigateBack,
            onAnalyticsToggleChanged = onAnalyticsToggleChanged,
            onViewModeChanged = onViewModeChanged,
        )
    }
}
