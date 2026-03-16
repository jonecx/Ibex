package com.jonecx.ibex.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jonecx.ibex.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HomeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
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
        composeTestRule.onNodeWithText("Cloud").assertIsDisplayed()
        composeTestRule.onNodeWithText("SMB/CIFS").assertIsDisplayed()
        composeTestRule.onNodeWithText("FTP").assertIsDisplayed()
    }
}
