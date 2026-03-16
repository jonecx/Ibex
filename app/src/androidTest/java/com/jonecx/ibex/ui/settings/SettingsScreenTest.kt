package com.jonecx.ibex.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import com.jonecx.ibex.util.runOnUiThreadBlocking
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class SettingsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var fakePreferences: FakeSettingsPreferences

    @Before
    fun setup() {
        hiltRule.inject()
        fakePreferences.reset()
    }

    private fun navigateToSettings() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun displaysTitle() {
        navigateToSettings()

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun displaysBackButton() {
        navigateToSettings()

        composeTestRule.onNodeWithContentDescription("Navigate up").assertIsDisplayed()
    }

    @Test
    fun displaysAnalyticsToggle() {
        navigateToSettings()

        composeTestRule.onNodeWithText("Send Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send anonymous usage data to help improve the app")
            .assertIsDisplayed()
    }

    @Test
    fun backButtonReturnsToHome() {
        navigateToSettings()

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Ibex").assertIsDisplayed()
    }

    @Test
    fun analyticsToggleStartsOff() {
        navigateToSettings()

        composeTestRule.onNodeWithText("Send Analytics").assertIsDisplayed()
    }

    @Test
    fun analyticsToggleCanBeEnabled() {
        navigateToSettings()

        composeTestRule.onNodeWithText("Send Analytics").performClick()
        composeTestRule.waitForIdle()

        assertEquals(true, fakePreferences.currentAnalyticsValue())
    }

    @Test
    fun analyticsToggleCanBeDisabled() {
        composeTestRule.runOnUiThreadBlocking {
            fakePreferences.setSendAnalyticsEnabled(true)
        }
        navigateToSettings()

        composeTestRule.onNodeWithText("Send Analytics").performClick()
        composeTestRule.waitForIdle()

        assertEquals(false, fakePreferences.currentAnalyticsValue())
    }

    @Test
    fun viewModeRadioGroupIsDisplayed() {
        navigateToSettings()

        composeTestRule.onNodeWithText("View Mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("List").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grid").assertIsDisplayed()
    }

    @Test
    fun viewModeDefaultIsListSelected() {
        navigateToSettings()

        composeTestRule.onNodeWithText("View Mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("List").assertIsDisplayed()
    }

    @Test
    fun viewModeGridClickUpdatesPreference() {
        navigateToSettings()

        composeTestRule.onNodeWithText("Grid").performClick()
        composeTestRule.waitForIdle()

        assertEquals(ViewMode.GRID, fakePreferences.currentViewMode())
    }

    @Test
    fun viewModeListClickUpdatesPreference() {
        composeTestRule.runOnUiThreadBlocking {
            fakePreferences.setViewMode(ViewMode.GRID)
        }
        navigateToSettings()

        composeTestRule.onNodeWithText("List").performClick()
        composeTestRule.waitForIdle()

        assertEquals(ViewMode.LIST, fakePreferences.currentViewMode())
    }

    @Test
    fun gridColumnsSliderHiddenInListMode() {
        navigateToSettings()

        composeTestRule.onNodeWithText("Grid Columns").assertDoesNotExist()
    }

    @Test
    fun gridColumnsSliderShownInGridMode() {
        composeTestRule.runOnUiThreadBlocking {
            fakePreferences.setViewMode(ViewMode.GRID)
        }
        navigateToSettings()

        composeTestRule.onNodeWithText("Grid Columns").assertIsDisplayed()
    }

    @Test
    fun gridColumnsSliderDisplaysAllStepLabels() {
        composeTestRule.runOnUiThreadBlocking {
            fakePreferences.setViewMode(ViewMode.GRID)
        }
        navigateToSettings()

        listOf("2", "3", "4", "5", "6").forEach {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }
    }
}
