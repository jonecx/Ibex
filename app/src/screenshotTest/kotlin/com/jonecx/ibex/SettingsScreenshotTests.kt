package com.jonecx.ibex

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.jonecx.ibex.ui.settings.SettingsScreenContent
import com.jonecx.ibex.ui.settings.SettingsUiState
import com.jonecx.ibex.ui.theme.IbexTheme

@Composable
private fun SettingsPreview(
    uiState: SettingsUiState = SettingsUiState(),
) {
    SettingsScreenContent(
        uiState = uiState,
        onNavigateBack = {},
        onAnalyticsToggleChanged = {},
        onViewModeChanged = {},
        onGridColumnsChanged = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsScreenAnalyticsEnabledPreview() {
    IbexTheme {
        SettingsPreview(uiState = SettingsUiState(sendAnalyticsEnabled = true))
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsScreenAnalyticsDisabledPreview() {
    IbexTheme {
        SettingsPreview(uiState = SettingsUiState(sendAnalyticsEnabled = false))
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenDarkThemePreview() {
    IbexTheme(darkTheme = true) {
        SettingsPreview(uiState = SettingsUiState(sendAnalyticsEnabled = true))
    }
}
