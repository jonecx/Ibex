package com.jonecx.ibex

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.jonecx.ibex.fixtures.HomeStatsFixtures
import com.jonecx.ibex.ui.home.HomeScreenContent
import com.jonecx.ibex.ui.theme.IbexTheme

@Composable
private fun HomePreview() {
    HomeScreenContent(
        localSources = HomeStatsFixtures.localSources,
        remoteSources = HomeStatsFixtures.remoteSources,
        stats = HomeStatsFixtures.sample.sources,
        storageUsage = HomeStatsFixtures.sample.storageUsage,
        onSourceSelected = {},
        onSettingsClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    IbexTheme {
        HomePreview()
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenDarkThemePreview() {
    IbexTheme(darkTheme = true) {
        HomePreview()
    }
}
