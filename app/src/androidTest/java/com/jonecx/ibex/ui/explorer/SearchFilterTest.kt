package com.jonecx.ibex.ui.explorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import coil.Coil
import coil.ImageLoader
import com.jonecx.ibex.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class SearchFilterTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var imageLoader: ImageLoader

    @Before
    fun setup() {
        hiltRule.inject()
        Coil.setImageLoader(imageLoader)
    }

    private fun navigateToStorage() {
        composeTestRule.onNodeWithText("Storage").performClick()
        composeTestRule.waitForIdle()
    }

    private fun openSearch() {
        composeTestRule.onNodeWithContentDescription("Search").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun searchFiltersFilesByName() {
        navigateToStorage()

        composeTestRule.onNodeWithText("Alarms").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download").assertIsDisplayed()

        openSearch()
        composeTestRule.onNodeWithText("Search files…").assertIsDisplayed()

        composeTestRule.onNodeWithText("Search files…").performTextInput("Down")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Download").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alarms").assertDoesNotExist()
    }

    @Test
    fun searchShowsAllFilesWithEmptyQuery() {
        navigateToStorage()
        openSearch()

        composeTestRule.onNodeWithText("Alarms").assertIsDisplayed()
        composeTestRule.onNodeWithText("Android").assertIsDisplayed()
        composeTestRule.onNodeWithText("DCIM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download").assertIsDisplayed()
    }

    @Test
    fun searchDismissedByCloseButton() {
        navigateToStorage()
        openSearch()

        composeTestRule.onNodeWithText("Search files…").performTextInput("xyz")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Cancel").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Alarms").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download").assertIsDisplayed()
    }

    @Test
    fun searchClearsWhenNavigatingIntoDirectory() {
        navigateToStorage()
        openSearch()

        composeTestRule.onNodeWithText("Search files…").performTextInput("D")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("DCIM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download").assertIsDisplayed()

        composeTestRule.onNodeWithText("DCIM").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
    }

    @Test
    fun searchIsCaseInsensitive() {
        navigateToStorage()
        openSearch()

        composeTestRule.onNodeWithText("Search files…").performTextInput("dcim")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("DCIM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alarms").assertDoesNotExist()
    }
}
