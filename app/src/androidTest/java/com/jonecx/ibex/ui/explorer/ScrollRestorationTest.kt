package com.jonecx.ibex.ui.explorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import coil.Coil
import coil.ImageLoader
import com.jonecx.ibex.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.inject

class ScrollRestorationTest : KoinTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val imageLoader: ImageLoader by inject()

    @Before
    fun setup() {
        Coil.setImageLoader(imageLoader)
    }

    private fun navigateToStorageDcim() {
        composeTestRule.onNodeWithText("Storage").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("DCIM").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun scrollPositionRestoredAfterNavigatingBackFromSubdirectory() {
        navigateToStorageDcim()

        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()

        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(3)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Screenshots").assertIsDisplayed()

        composeTestRule.onNodeWithText("Screenshots").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Screenshots").assertIsDisplayed()
    }

    @Test
    fun scrollPositionResetsWhenEnteringNewDirectory() {
        navigateToStorageDcim()

        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(15)
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Camera").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Events").assertIsDisplayed()
    }
}
