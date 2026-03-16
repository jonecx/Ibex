package com.jonecx.ibex.ui.network

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.data.model.NetworkProtocol
import com.jonecx.ibex.fixtures.FakeNetworkConnectionsPreferences
import com.jonecx.ibex.util.runOnUiThreadBlocking
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class AddNetworkConnectionScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var fakePreferences: FakeNetworkConnectionsPreferences

    @Before
    fun setup() {
        hiltRule.inject()
        fakePreferences.reset()
    }

    private fun navigateToAddConnection(tileName: String = "SMB/CIFS") {
        composeTestRule.onNodeWithText(tileName).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Add connection").performClick()
        composeTestRule.waitForIdle()
    }

    private fun seedConnections(vararg connections: NetworkConnection) {
        composeTestRule.runOnUiThreadBlocking {
            connections.forEach { fakePreferences.addConnection(it) }
        }
    }

    @Test
    fun displaysAddConnectionTitle() {
        navigateToAddConnection()

        composeTestRule.onNodeWithText("Add Connection").assertIsDisplayed()
    }

    @Test
    fun displaysEditConnectionTitleWhenEditing() {
        val connection = NetworkConnection(
            id = "edit-1",
            displayName = "Edit Me",
            host = "192.168.1.1",
            username = "user",
        )
        seedConnections(connection)
        composeTestRule.onNodeWithText("SMB/CIFS").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Edit connection").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Edit Connection").assertIsDisplayed()
    }

    @Test
    fun displaysProtocolDropdown() {
        navigateToAddConnection()

        composeTestRule.onNodeWithText("Protocol").assertIsDisplayed()
    }

    @Test
    fun smbProtocolShowsHostLabel() {
        navigateToAddConnection(tileName = "SMB/CIFS")

        composeTestRule.onNodeWithText("Host").assertIsDisplayed()
    }

    @Test
    fun cloudProtocolShowsUrlField() {
        navigateToAddConnection(tileName = "Cloud")

        composeTestRule.onNodeWithText("URL / Hostname").assertIsDisplayed()
    }

    @Test
    fun smbProtocolShowsAnonymousCheckbox() {
        navigateToAddConnection(tileName = "SMB/CIFS")

        composeTestRule.onNodeWithText("Anonymous").assertIsDisplayed()
    }

    @Test
    fun cloudProtocolDoesNotShowAnonymousCheckbox() {
        navigateToAddConnection(tileName = "Cloud")

        composeTestRule.onNodeWithText("Anonymous").assertDoesNotExist()
    }

    @Test
    fun displaysPortField() {
        navigateToAddConnection()

        composeTestRule.onNodeWithText("Port").assertIsDisplayed()
    }

    @Test
    fun smbDefaultPortIs445() {
        navigateToAddConnection(tileName = "SMB/CIFS")

        composeTestRule.onNodeWithText("445").assertIsDisplayed()
    }

    @Test
    fun ftpDefaultPortIs21() {
        navigateToAddConnection(tileName = "FTP")

        composeTestRule.onNodeWithText("21").assertIsDisplayed()
    }

    @Test
    fun cloudDefaultPortIs443() {
        navigateToAddConnection(tileName = "Cloud")

        composeTestRule.onNodeWithText("443").assertIsDisplayed()
    }

    @Test
    fun displaysUsernameField() {
        navigateToAddConnection()

        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
    }

    @Test
    fun displaysPasswordField() {
        navigateToAddConnection()

        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
    }

    @Test
    fun displaysDisplayNameField() {
        navigateToAddConnection()

        composeTestRule.onNodeWithText("Display name").assertIsDisplayed()
    }

    @Test
    fun saveButtonDisabledWhenFormEmpty() {
        navigateToAddConnection(tileName = "Cloud")

        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun saveButtonEnabledWhenCloudFormValid() {
        navigateToAddConnection(tileName = "Cloud")

        composeTestRule.onNodeWithText("URL / Hostname").performTextInput("cloud.example.com")
        composeTestRule.onNodeWithText("Username").performTextInput("user")

        composeTestRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun saveCreatesConnectionAndReturnsToList() {
        navigateToAddConnection(tileName = "Cloud")

        composeTestRule.onNodeWithText("URL / Hostname").performTextInput("cloud.example.com")
        composeTestRule.onNodeWithText("Username").performTextInput("testuser")
        composeTestRule.onNodeWithText("Display name").performTextInput("My Cloud")

        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Network Connections").assertIsDisplayed()
        composeTestRule.onNodeWithText("My Cloud").assertIsDisplayed()
        composeTestRule.onNodeWithText("cloud.example.com").assertIsDisplayed()
    }

    @Test
    fun editModePopulatesFields() {
        val connection = NetworkConnection(
            id = "edit-1",
            protocol = NetworkProtocol.FTP,
            displayName = "My FTP",
            host = "192.168.1.50",
            port = 2121,
            username = "ftpuser",
        )
        seedConnections(connection)
        composeTestRule.onNodeWithText("FTP").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Edit connection").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("My FTP").assertIsDisplayed()
        composeTestRule.onNodeWithText("ftpuser").assertIsDisplayed()
        composeTestRule.onNodeWithText("2121").assertIsDisplayed()
    }

    @Test
    fun cancelButtonReturnsToConnectionsList() {
        navigateToAddConnection()

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Network Connections").assertIsDisplayed()
    }

    @Test
    fun backButtonReturnsToConnectionsList() {
        navigateToAddConnection()

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Network Connections").assertIsDisplayed()
    }
}
