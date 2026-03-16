package com.jonecx.ibex.ui.network

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.fixtures.FakeNetworkConnectionsPreferences
import com.jonecx.ibex.fixtures.NetworkConnectionFixtures.cloudConnection
import com.jonecx.ibex.fixtures.NetworkConnectionFixtures.ftpConnection
import com.jonecx.ibex.fixtures.NetworkConnectionFixtures.smbConnection
import com.jonecx.ibex.util.runOnUiThreadBlocking
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class NetworkConnectionsScreenTest {

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

    private fun seedConnections(vararg connections: NetworkConnection) {
        composeTestRule.runOnUiThreadBlocking {
            connections.forEach { fakePreferences.addConnection(it) }
        }
    }

    private fun navigateToNetworkConnections() {
        composeTestRule.onNodeWithText("SMB/CIFS").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun emptyStateShowsNoConnectionsMessage() {
        navigateToNetworkConnections()

        composeTestRule.onNodeWithText("No connections yet.", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysNetworkConnectionsTitle() {
        navigateToNetworkConnections()

        composeTestRule.onNodeWithText("Network Connections").assertIsDisplayed()
    }

    @Test
    fun showsConnectionDisplayName() {
        seedConnections(smbConnection)
        navigateToNetworkConnections()

        composeTestRule.onNodeWithText("Office NAS").assertIsDisplayed()
    }

    @Test
    fun showsConnectionHost() {
        seedConnections(smbConnection)
        navigateToNetworkConnections()

        composeTestRule.onNodeWithText("192.168.1.100").assertIsDisplayed()
    }

    @Test
    fun showsConnectionUsername() {
        seedConnections(smbConnection)
        navigateToNetworkConnections()

        composeTestRule.onNodeWithText("admin").assertIsDisplayed()
    }

    @Test
    fun showsSectionHeadersForMultipleProtocols() {
        seedConnections(smbConnection, ftpConnection, cloudConnection)
        navigateToNetworkConnections()

        composeTestRule.onNodeWithText("SMB/CIFS", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("FTP", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cloud", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun showsAllConnectionsGroupedByProtocol() {
        seedConnections(smbConnection, ftpConnection, cloudConnection)
        navigateToNetworkConnections()

        composeTestRule.onNodeWithText("Office NAS").assertIsDisplayed()
        composeTestRule.onNodeWithText("FTP Server").assertIsDisplayed()
        composeTestRule.onNodeWithText("My Cloud").assertIsDisplayed()
    }

    @Test
    fun deleteDialogShowsOnDeleteClick() {
        seedConnections(smbConnection)
        navigateToNetworkConnections()

        composeTestRule.onNodeWithContentDescription("Delete connection").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete connection?").assertIsDisplayed()
        composeTestRule.onNodeWithText("\"Office NAS\" will be removed.").assertIsDisplayed()
    }

    @Test
    fun deleteDialogDismissesOnCancel() {
        seedConnections(smbConnection)
        navigateToNetworkConnections()

        composeTestRule.onNodeWithContentDescription("Delete connection").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete connection?").assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Delete connection?").assertDoesNotExist()
    }

    @Test
    fun deleteConfirmRemovesConnection() {
        seedConnections(smbConnection)
        navigateToNetworkConnections()

        composeTestRule.onNodeWithContentDescription("Delete connection").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Office NAS").assertDoesNotExist()
        composeTestRule.onNodeWithText("No connections yet.", substring = true).assertIsDisplayed()
    }

    @Test
    fun anonymousConnectionDoesNotShowUsername() {
        val anonConnection = smbConnection.copy(anonymous = true, username = "")
        seedConnections(anonConnection)
        navigateToNetworkConnections()

        composeTestRule.onNodeWithText("Office NAS").assertIsDisplayed()
        composeTestRule.onNodeWithText("admin").assertDoesNotExist()
    }

    @Test
    fun editClickNavigatesToEditScreen() {
        seedConnections(smbConnection)
        navigateToNetworkConnections()

        composeTestRule.onNodeWithContentDescription("Edit connection").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Edit Connection").assertIsDisplayed()
        composeTestRule.onNodeWithText("Office NAS").assertIsDisplayed()
        composeTestRule.onNodeWithText("admin").assertIsDisplayed()
    }
}
