package com.jonecx.ibex.ui.navigation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonecx.ibex.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testStorageTileNavigatesToFileExplorer() {
        composeTestRule.onNodeWithText("Storage").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Alarms").assertIsDisplayed()
        composeTestRule.onNodeWithText("Android").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audiobooks").assertIsDisplayed()
        composeTestRule.onNodeWithText("DCIM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Documents").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("DIRECTORY").assertCountEquals(6)
    }

    @Test
    fun testDownloadsTileNavigatesToDownloadsFolder() {
        composeTestRule.onNodeWithText("Downloads").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Audio 1.mp3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio Flac 1.zip").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coffee.jpg").assertIsDisplayed()
        composeTestRule.onNodeWithText("CS201-DS.ppt").assertIsDisplayed()
        composeTestRule.onNodeWithText("CS8391-DS.docx").assertIsDisplayed()
        composeTestRule.onNodeWithText("Headphone.mp4").assertIsDisplayed()
        composeTestRule.onNodeWithText("PDF_DS.pdf").assertIsDisplayed()
    }

    @Test
    fun testStorageShowsFolderItemCounts() {
        composeTestRule.onNodeWithText("Storage").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("0 items", useUnmergedTree = true).assertCountEquals(4)
        composeTestRule.onNodeWithText("3 items", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("15 items", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testDownloadsShowsFileSizes() {
        composeTestRule.onNodeWithText("Downloads").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("118.8 KB", substring = true, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("1.6 MB", substring = true, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testImagesTileNavigatesToImages() {
        composeTestRule.onNodeWithText("Images").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sunset.jpg").assertIsDisplayed()
        composeTestRule.onNodeWithText("Portrait.png").assertIsDisplayed()
        composeTestRule.onNodeWithText("Screenshot_2024.png").assertIsDisplayed()
    }

    @Test
    fun testVideosTileNavigatesToVideos() {
        composeTestRule.onNodeWithText("Videos").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Birthday.mp4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tutorial.mkv").assertIsDisplayed()
    }

    @Test
    fun testAudioTileNavigatesToAudio() {
        composeTestRule.onNodeWithText("Audio").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Song1.mp3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Podcast.m4a").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ringtone.ogg").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("AUDIO").assertCountEquals(3)
    }

    @Test
    fun testDocumentsTileNavigatesToDocuments() {
        composeTestRule.onNodeWithText("Documents").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Report.pdf").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notes.txt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spreadsheet.xlsx").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("DOCUMENT").assertCountEquals(3)
    }

    @Test
    fun testAppsTileNavigatesToApps() {
        composeTestRule.onNodeWithText("Apps").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Calculator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("APK").assertCountEquals(3)
    }

    @Test
    fun testRecentTileNavigatesToRecent() {
        composeTestRule.onNodeWithText("Recent").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("RecentDoc.pdf").assertIsDisplayed()
        composeTestRule.onNodeWithText("RecentPhoto.jpg").assertIsDisplayed()
    }

    @Test
    fun testTrashTileNavigatesToTrash() {
        composeTestRule.onNodeWithText("Trash").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("DeletedFile.txt").assertIsDisplayed()
        composeTestRule.onNodeWithText("OldPhoto.jpg").assertIsDisplayed()
    }
}
