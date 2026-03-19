package com.jonecx.ibex.ui.explorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
class SortIntegrationTest {

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

    private fun navigateToDownloads() {
        composeTestRule.onNodeWithText("Storage").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Download").performClick()
        composeTestRule.waitForIdle()
    }

    private fun openSortSheet() {
        composeTestRule.onNodeWithContentDescription("Sort").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun defaultSortIsNameAscending() {
        navigateToDownloads()

        // First file alphabetically should be visible at top
        composeTestRule.onNodeWithText("Audio 1.mp3").assertIsDisplayed()
    }

    @Test
    fun sortBySizeAscendingShowsSmallestFirst() {
        navigateToDownloads()
        openSortSheet()

        composeTestRule.onNodeWithText("Size").performClick()
        composeTestRule.waitForIdle()

        // Smallest file: CS8391-DS.docx (50,585 bytes)
        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("CS8391-DS.docx").assertIsDisplayed()
    }

    @Test
    fun sortBySizeDescendingShowsLargestFirst() {
        navigateToDownloads()
        openSortSheet()

        composeTestRule.onNodeWithText("Size").performClick()
        composeTestRule.waitForIdle()

        // Switch to descending
        composeTestRule.onAllNodesWithText("▼ Descending")[0].performClick()
        composeTestRule.waitForIdle()

        // Largest file: Headphone.mp4 (115,343,360 bytes)
        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Headphone.mp4").assertIsDisplayed()
    }

    @Test
    fun sortByNameDescendingReversesOrder() {
        navigateToDownloads()
        openSortSheet()

        composeTestRule.onAllNodesWithText("▼ Descending")[0].performClick()
        composeTestRule.waitForIdle()

        // Last alphabetically: PDF_DS.pdf
        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("PDF_DS.pdf").assertIsDisplayed()
    }

    @Test
    fun sortPersistsAcrossDirectoryNavigation() {
        navigateToDownloads()
        openSortSheet()

        composeTestRule.onNodeWithText("Size").performClick()
        composeTestRule.waitForIdle()

        // Navigate back to storage
        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        composeTestRule.waitForIdle()

        // Navigate back into Downloads
        composeTestRule.onNodeWithText("Download").performClick()
        composeTestRule.waitForIdle()

        // Sort should still be size ascending — smallest first
        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("CS8391-DS.docx").assertIsDisplayed()
    }
}
