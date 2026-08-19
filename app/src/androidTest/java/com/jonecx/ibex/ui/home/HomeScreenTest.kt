package com.jonecx.ibex.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.util.scrollToHomeTile
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
    }

    @Test
    fun displaysAppName() {
        composeTestRule.onNodeWithText("Ibex").assertIsDisplayed()
    }

    @Test
    fun displaysLocalSection() {
        composeTestRule.onNodeWithText("Local").assertIsDisplayed()
    }

    @Test
    fun displaysRemoteSection() {
        composeTestRule.scrollToHomeTile("Remote")
        composeTestRule.onNodeWithText("Remote").assertIsDisplayed()
    }

    @Test
    fun displaysAllLocalSourceTiles() {
        composeTestRule.onNodeWithText("Storage").assertIsDisplayed()
        composeTestRule.onNodeWithText("Downloads").assertIsDisplayed()
        composeTestRule.onNodeWithText("Images").assertIsDisplayed()
        composeTestRule.onNodeWithText("Videos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Documents").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apps").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trash").assertIsDisplayed()
    }

    @Test
    fun displaysAllRemoteSourceTiles() {
        composeTestRule.scrollToHomeTile("Cloud")
        composeTestRule.onNodeWithText("Cloud").assertIsDisplayed()
        composeTestRule.scrollToHomeTile("SMB/CIFS")
        composeTestRule.onNodeWithText("SMB/CIFS").assertIsDisplayed()
        composeTestRule.scrollToHomeTile("FTP")
        composeTestRule.onNodeWithText("FTP").assertIsDisplayed()
    }

    @Test
    fun displaysSizeAndCountAcrossLocalTiles() {
        // Counts come from the fake stats repository wired into the test Koin graph.
        composeTestRule.onNodeWithText("(4253)", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("(1056)", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("(364)", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("(172)", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysUsedOverTotalOnStorageTile() {
        composeTestRule.onNodeWithText("221.0 GB / 256.0 GB").assertIsDisplayed()
    }
}
