package com.jonecx.ibex.ui.explorer

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil.Coil
import coil.ImageLoader
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import com.jonecx.ibex.util.runOnUiThreadBlocking
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class FileExplorerViewModeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var fakePreferences: FakeSettingsPreferences

    @Inject
    lateinit var imageLoader: ImageLoader

    @Before
    fun setup() {
        hiltRule.inject()
        fakePreferences.reset()
        Coil.setImageLoader(imageLoader)
    }

    @Test
    fun testStorageDefaultsToListView() {
        composeTestRule.onNodeWithText("Storage").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("file_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("file_grid").assertDoesNotExist()
        composeTestRule.onNodeWithText("Alarms").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0 items", useUnmergedTree = true).assertCountEquals(4)
    }

    @Test
    fun testStorageSwitchesToGridView() {
        composeTestRule.runOnUiThreadBlocking {
            fakePreferences.setViewMode(ViewMode.GRID)
        }

        composeTestRule.onNodeWithText("Storage").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("file_grid").assertIsDisplayed()
        composeTestRule.onNodeWithTag("file_list").assertDoesNotExist()
        composeTestRule.onNodeWithText("Alarms").assertIsDisplayed()
        composeTestRule.onNodeWithText("Android").assertIsDisplayed()
    }

    @Test
    fun testGridViewHidesTextForImagesWithThumbnail() {
        composeTestRule.runOnUiThreadBlocking {
            fakePreferences.setViewMode(ViewMode.GRID)
        }

        composeTestRule.onNodeWithText("Images").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("file_grid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sunset.jpg").assertDoesNotExist()
        composeTestRule.onNodeWithText("2.4 MB", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun testListViewShowsFileSizeForImages() {
        composeTestRule.runOnUiThreadBlocking {
            fakePreferences.setViewMode(ViewMode.LIST)
        }

        composeTestRule.onNodeWithText("Images").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("file_list").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sunset.jpg").assertIsDisplayed()
        composeTestRule.onNodeWithText("2.4 MB", substring = true, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testListViewShowsSubtextForFolders() {
        composeTestRule.runOnUiThreadBlocking {
            fakePreferences.setViewMode(ViewMode.LIST)
        }

        composeTestRule.onNodeWithText("Storage").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("file_list").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alarms").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0 items", useUnmergedTree = true).assertCountEquals(4)
    }

    @Test
    fun testViewModeSwitchesLiveWhileOnExplorer() {
        composeTestRule.onNodeWithText("Storage").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("file_list").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0 items", useUnmergedTree = true).assertCountEquals(4)

        composeTestRule.runOnUiThreadBlocking {
            fakePreferences.setViewMode(ViewMode.GRID)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("file_grid").assertIsDisplayed()
        composeTestRule.onNodeWithTag("file_list").assertDoesNotExist()
        composeTestRule.onNodeWithText("Alarms").assertIsDisplayed()
    }
}
