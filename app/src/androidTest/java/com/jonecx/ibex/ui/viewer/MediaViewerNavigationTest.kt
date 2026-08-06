package com.jonecx.ibex.ui.viewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil.Coil
import coil.ImageLoader
import com.jonecx.ibex.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.inject

class MediaViewerNavigationTest : KoinTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val imageLoader: ImageLoader by inject()

    @Before
    fun setup() {
        Coil.setImageLoader(imageLoader)
    }

    private fun navigateTo(source: String) {
        composeTestRule.onNodeWithText(source).performClick()
        composeTestRule.waitForIdle()
    }

    private fun tapFile(name: String) {
        composeTestRule.onNodeWithText(name).performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testTapImageOpensViewerAndShowsFilename() {
        navigateTo("Images")
        tapFile("Sunset.jpg")

        composeTestRule.onNodeWithText("Sunset.jpg").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 / 3").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    }

    @Test
    fun testTapSecondImageShowsCorrectIndex() {
        navigateTo("Images")
        tapFile("Portrait.png")

        composeTestRule.onNodeWithText("Portrait.png").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 / 3").assertIsDisplayed()
    }

    @Test
    fun testCloseButtonNavigatesBackToFileList() {
        navigateTo("Images")
        tapFile("Sunset.jpg")

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sunset.jpg").assertIsDisplayed()
        composeTestRule.onNodeWithText("Portrait.png").assertIsDisplayed()
        composeTestRule.onNodeWithText("Screenshot_2024.png").assertIsDisplayed()
    }

    @Test
    fun testTapVideoOpensViewer() {
        navigateTo("Downloads")
        tapFile("Headphone.mp4")

        composeTestRule.onNodeWithText("Headphone.mp4").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 / 2").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    }
}
