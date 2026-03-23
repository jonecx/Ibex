package com.jonecx.ibex.ui.explorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil.Coil
import coil.ImageLoader
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.data.model.RecentFolder
import com.jonecx.ibex.fixtures.FakeRecentFoldersPreferences
import com.jonecx.ibex.util.runOnUiThreadBlocking
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class RecentFoldersTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var fakeRecentFolders: FakeRecentFoldersPreferences

    @Inject
    lateinit var imageLoader: ImageLoader

    @Before
    fun setup() {
        hiltRule.inject()
        Coil.setImageLoader(imageLoader)
    }

    private fun recentFolder(path: String, displayName: String) = RecentFolder(
        path = path,
        displayName = displayName,
        timestamp = System.currentTimeMillis(),
        sourceType = "LOCAL_STORAGE",
    )

    private fun seedRecent(path: String, displayName: String) {
        composeTestRule.runOnUiThreadBlocking {
            fakeRecentFolders.addRecentFolder(recentFolder(path, displayName))
        }
    }

    private fun navigateToStorage() {
        composeTestRule.onNodeWithText("Storage").performClick()
        composeTestRule.waitForIdle()
    }

    private fun openRecentsSheet() {
        composeTestRule.onNodeWithContentDescription("Recent Folders").performClick()
        composeTestRule.waitForIdle()
    }

    private fun assertSheetHasFolder(displayName: String, path: String) {
        composeTestRule.onNodeWithTag("recent_folders_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText(path, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun recentsSheetShowsEmptyState() {
        navigateToStorage()
        openRecentsSheet()

        composeTestRule.onNodeWithText("Recent Folders").assertIsDisplayed()
        composeTestRule.onNodeWithText("No recent folders yet").assertIsDisplayed()
    }

    @Test
    fun recentsSheetShowsPrePopulatedFolders() {
        seedRecent("/storage/emulated/0/DCIM", "DCIM")
        seedRecent("/storage/emulated/0/Download", "Download")

        navigateToStorage()
        openRecentsSheet()

        composeTestRule.onNodeWithText("Recent Folders").assertIsDisplayed()
        assertSheetHasFolder("DCIM", "/storage/emulated/0/DCIM")
        assertSheetHasFolder("Download", "/storage/emulated/0/Download")
    }

    @Test
    fun recentsSheetShowsClearButton() {
        seedRecent("/storage/emulated/0/Music", "Music")

        navigateToStorage()
        openRecentsSheet()

        composeTestRule.onNodeWithText("Clear").assertIsDisplayed()
    }

    @Test
    fun clearButtonRemovesAllRecents() {
        seedRecent("/storage/emulated/0/Music", "Music")

        navigateToStorage()
        openRecentsSheet()

        composeTestRule.onNodeWithText("Music").assertIsDisplayed()

        composeTestRule.onNodeWithText("Clear").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("No recent folders yet").assertIsDisplayed()
    }

    @Test
    fun navigatingToDirectoryAddsToRecents() {
        navigateToStorage()

        composeTestRule.onNodeWithText("Download").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        composeTestRule.waitForIdle()

        openRecentsSheet()

        assertSheetHasFolder("Download", "/storage/emulated/0/Download")
    }

    @Test
    fun tappingRecentFolderNavigatesToIt() {
        seedRecent("/storage/emulated/0/Download", "Download")

        navigateToStorage()
        openRecentsSheet()

        composeTestRule.onNodeWithText("/storage/emulated/0/Download", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Audio 1.mp3").assertIsDisplayed()
    }
}
