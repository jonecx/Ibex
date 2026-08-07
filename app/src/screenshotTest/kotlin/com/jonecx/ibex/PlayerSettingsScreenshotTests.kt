package com.jonecx.ibex

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.jonecx.ibex.data.preferences.PlayerSettingsPreferencesContract
import com.jonecx.ibex.ui.settings.PlayerSettingsScreenContent
import com.jonecx.ibex.ui.theme.IbexTheme

@Composable
private fun PlayerSettingsPreview() {
    PlayerSettingsScreenContent(
        settings = PlayerSettingsPreferencesContract.DEFAULTS,
        onNavigateBack = {},
        onUpdate = {},
        onReset = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PlayerSettingsScreenLightPreview() {
    IbexTheme(darkTheme = false) {
        PlayerSettingsPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PlayerSettingsScreenDarkPreview() {
    IbexTheme(darkTheme = true) {
        PlayerSettingsPreview()
    }
}
