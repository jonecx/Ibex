package com.jonecx.ibex.ui.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.jonecx.ibex.util.setIbexContent
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        composeTestRule.setIbexContent {
            HomeScreen(onSourceSelected = {}, onSettingsClick = {})
        }
    }

    @Test
    fun testHomeScreenDisplaysAppName() {
        composeTestRule.onNodeWithText("Ibex").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenDisplaysLocalSection() {
        composeTestRule.onNodeWithText("Local").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenDisplaysRemoteSection() {
        composeTestRule.onNodeWithText("Remote").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenDisplaysAllLocalSourceTiles() {
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
    fun testHomeScreenDisplaysAllRemoteSourceTiles() {
        composeTestRule.onNodeWithText("Cloud").assertIsDisplayed()
        composeTestRule.onNodeWithText("SMB/CIFS").assertIsDisplayed()
        composeTestRule.onNodeWithText("FTP").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenDisplaysComingSoonForRemoteSources() {
        composeTestRule.onAllNodesWithText("Coming soon", useUnmergedTree = true)
            .assertCountEquals(3)
    }
}
