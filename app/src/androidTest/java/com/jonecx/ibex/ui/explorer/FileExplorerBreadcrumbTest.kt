package com.jonecx.ibex.ui.explorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil.Coil
import coil.ImageLoader
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.inject

class FileExplorerBreadcrumbTest : KoinTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val fakePreferences: FakeSettingsPreferences by inject()
    private val imageLoader: ImageLoader by inject()

    @Before
    fun setup() {
        fakePreferences.reset()
        Coil.setImageLoader(imageLoader)
    }

    @Test
    fun breadcrumbHiddenAtRoot() {
        composeTestRule.onNodeWithText("Storage").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("breadcrumb_bar").assertDoesNotExist()
    }

    @Test
    fun breadcrumbAppearsAfterEnteringFolder() {
        composeTestRule.onNodeWithText("Storage").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("DCIM").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("breadcrumb_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("breadcrumb_DCIM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
    }

    @Test
    fun tappingHomeCrumbReturnsToRoot() {
        composeTestRule.onNodeWithText("Storage").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("DCIM").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()

        composeTestRule.onNodeWithTag("breadcrumb_home").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Alarms").assertIsDisplayed()
        composeTestRule.onNodeWithTag("breadcrumb_bar").assertDoesNotExist()
    }
}
