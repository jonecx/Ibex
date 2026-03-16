package com.jonecx.ibex.ui.network

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonecx.ibex.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NetworkConnectionsNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    private fun navigateToConnections(tileName: String = "SMB/CIFS") {
        composeTestRule.onNodeWithText(tileName).performClick()
        composeTestRule.waitForIdle()
    }

    private fun navigateToAddForm(tileName: String = "SMB/CIFS") {
        navigateToConnections(tileName)
        composeTestRule.onNodeWithContentDescription("Add connection").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun smbTileNavigatesToNetworkConnectionsScreen() {
        navigateToConnections("SMB/CIFS")

        composeTestRule.onNodeWithText("Network Connections").assertIsDisplayed()
    }

    @Test
    fun ftpTileNavigatesToNetworkConnectionsScreen() {
        navigateToConnections("FTP")

        composeTestRule.onNodeWithText("Network Connections").assertIsDisplayed()
    }

    @Test
    fun cloudTileNavigatesToNetworkConnectionsScreen() {
        navigateToConnections("Cloud")

        composeTestRule.onNodeWithText("Network Connections").assertIsDisplayed()
    }

    @Test
    fun networkConnectionsShowsEmptyState() {
        navigateToConnections()

        composeTestRule.onNodeWithText("No connections yet.", substring = true).assertIsDisplayed()
    }

    @Test
    fun addButtonNavigatesToAddConnectionScreen() {
        navigateToAddForm()

        composeTestRule.onNodeWithText("Add Connection").assertIsDisplayed()
    }

    @Test
    fun smbTilePreselectsSmbProtocolInAddForm() {
        navigateToAddForm("SMB/CIFS")

        composeTestRule.onNodeWithText("SMB").assertIsDisplayed()
        composeTestRule.onNodeWithText("445").assertIsDisplayed()
        composeTestRule.onNodeWithText("Host").assertIsDisplayed()
    }

    @Test
    fun ftpTilePreselectsFtpProtocolInAddForm() {
        navigateToAddForm("FTP")

        composeTestRule.onNodeWithText("FTP", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("21").assertIsDisplayed()
        composeTestRule.onNodeWithText("Host").assertIsDisplayed()
    }

    @Test
    fun cloudTilePreselectsCloudProtocolInAddForm() {
        navigateToAddForm("Cloud")

        composeTestRule.onNodeWithText("Cloud", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("443").assertIsDisplayed()
        composeTestRule.onNodeWithText("URL / Hostname").assertIsDisplayed()
    }

    @Test
    fun backButtonFromConnectionsReturnsToHome() {
        navigateToConnections()

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Ibex").assertIsDisplayed()
    }

    @Test
    fun backButtonFromAddFormReturnsToConnections() {
        navigateToAddForm()

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Network Connections").assertIsDisplayed()
    }
}
