package com.jonecx.ibex.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonecx.ibex.util.setSettingsContent
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsScreenDisplaysTitle() {
        composeTestRule.setSettingsContent()

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun testSettingsScreenDisplaysBackButton() {
        composeTestRule.setSettingsContent()

        composeTestRule.onNodeWithContentDescription("Navigate up").assertIsDisplayed()
    }

    @Test
    fun testSettingsScreenDisplaysAnalyticsToggle() {
        composeTestRule.setSettingsContent()

        composeTestRule.onNodeWithText("Send Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send anonymous usage data to help improve the app")
            .assertIsDisplayed()
    }

    @Test
    fun testBackButtonTriggersNavigation() {
        var navigatedBack = false

        composeTestRule.setSettingsContent(
            onNavigateBack = { navigatedBack = true },
        )

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        assert(navigatedBack) { "Expected onNavigateBack to be called" }
    }

    @Test
    fun testAnalyticsToggleStartsOff() {
        composeTestRule.setSettingsContent(
            uiState = SettingsUiState(sendAnalyticsEnabled = false),
        )

        composeTestRule.onNodeWithText("Send Analytics").assertIsDisplayed()
    }

    @Test
    fun testAnalyticsToggleCanBeEnabled() {
        var analyticsEnabled = false

        composeTestRule.setSettingsContent(
            uiState = SettingsUiState(sendAnalyticsEnabled = false),
            onAnalyticsToggleChanged = { analyticsEnabled = it },
        )

        composeTestRule.onNodeWithText("Send Analytics").performClick()
        composeTestRule.waitForIdle()

        assert(analyticsEnabled) { "Expected analytics to be enabled after click" }
    }

    @Test
    fun testAnalyticsToggleCanBeDisabled() {
        var analyticsEnabled = true

        composeTestRule.setSettingsContent(
            uiState = SettingsUiState(sendAnalyticsEnabled = true),
            onAnalyticsToggleChanged = { analyticsEnabled = it },
        )

        composeTestRule.onNodeWithText("Send Analytics").performClick()
        composeTestRule.waitForIdle()

        assert(!analyticsEnabled) { "Expected analytics to be disabled after click" }
    }
}
