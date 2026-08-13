package com.jonecx.ibex.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.jonecx.azmaree.player.model.PlayButtonPosition
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.fixtures.FakePlayerSettingsPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.inject

class PlayerSettingsScreenTest : KoinTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val fakePreferences: FakePlayerSettingsPreferences by inject()

    @Before
    fun setup() {
        fakePreferences.reset()
    }

    private fun navigateToPlayerSettings() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Player").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun playerSettingsEntryIsShownInSettings() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Player").assertIsDisplayed()
        composeTestRule.onNodeWithText("Playback, controls, buffering, and gestures")
            .assertIsDisplayed()
    }

    @Test
    fun displaysSectionsAndEntries() {
        navigateToPlayerSettings()

        composeTestRule.onNodeWithText("Layout").assertIsDisplayed()
        composeTestRule.onNodeWithText("Playback").assertIsDisplayed()
        composeTestRule.onNodeWithText("Seek step").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cast").assertIsDisplayed()
    }

    @Test
    fun displaysBackButton() {
        navigateToPlayerSettings()

        composeTestRule.onNodeWithContentDescription("Navigate up").assertIsDisplayed()
    }

    @Test
    fun seekStepChangeUpdatesPreference() {
        navigateToPlayerSettings()

        composeTestRule.onNodeWithText("30s").performClick()
        composeTestRule.waitForIdle()

        assertEquals(30_000L, fakePreferences.currentSettings().controls.seekStepMs)
    }

    @Test
    fun subtitlesToggleUpdatesPreference() {
        navigateToPlayerSettings()

        assertFalse(fakePreferences.currentSettings().controls.subtitlesEnabledByDefault)

        composeTestRule.onNodeWithText("Subtitles on by default").performClick()
        composeTestRule.waitForIdle()

        assertTrue(fakePreferences.currentSettings().controls.subtitlesEnabledByDefault)
    }

    @Test
    fun playButtonPositionChangeUpdatesPreference() {
        navigateToPlayerSettings()

        composeTestRule.onNodeWithContentDescription("Top start").performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            PlayButtonPosition.TOP_START,
            fakePreferences.currentSettings().controls.playButtonPosition,
        )
    }

    @Test
    fun resetRestoresDefaults() {
        navigateToPlayerSettings()

        composeTestRule.onNodeWithText("30s").performClick()
        composeTestRule.waitForIdle()
        assertEquals(30_000L, fakePreferences.currentSettings().controls.seekStepMs)

        composeTestRule.onNodeWithText("Reset to defaults").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(10_000L, fakePreferences.currentSettings().controls.seekStepMs)
    }
}
