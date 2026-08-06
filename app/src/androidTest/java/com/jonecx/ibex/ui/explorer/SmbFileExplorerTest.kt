package com.jonecx.ibex.ui.explorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import coil.Coil
import coil.ImageLoader
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.fixtures.FakeNetworkConnectionsPreferences
import com.jonecx.ibex.fixtures.NetworkConnectionFixtures
import com.jonecx.ibex.util.runOnUiThreadBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.inject

class SmbFileExplorerTest : KoinTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val fakeNetworkPreferences: FakeNetworkConnectionsPreferences by inject()
    private val imageLoader: ImageLoader by inject()

    @Before
    fun setup() {
        Coil.setImageLoader(imageLoader)
        composeTestRule.runOnUiThreadBlocking {
            fakeNetworkPreferences.reset()
        }
    }

    private fun addConnectionAndNavigateToExplorer() {
        composeTestRule.runOnUiThreadBlocking {
            fakeNetworkPreferences.addConnection(NetworkConnectionFixtures.smbConnection)
        }

        composeTestRule.onNodeWithText("SMB/CIFS").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(NetworkConnectionFixtures.smbConnection.displayName)
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun smbConnectionNavigatesToFileExplorer() {
        addConnectionAndNavigateToExplorer()

        composeTestRule.onNodeWithText(NetworkConnectionFixtures.smbConnection.displayName)
            .assertIsDisplayed()
    }

    @Test
    fun smbExplorerShowsFileList() {
        addConnectionAndNavigateToExplorer()

        composeTestRule.onNodeWithTag("file_list").assertIsDisplayed()
    }

    @Test
    fun smbExplorerShowsCreateFolderButton() {
        addConnectionAndNavigateToExplorer()

        composeTestRule.onNodeWithContentDescription("New folder").assertIsDisplayed()
    }

    @Test
    fun smbExplorerLongPressEntersSelectionMode() {
        addConnectionAndNavigateToExplorer()

        composeTestRule.onNodeWithText("Alarms").performTouchInput {
            longClick(center)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Cancel selection").assertIsDisplayed()
    }

    @Test
    fun smbExplorerBackNavigatesToConnections() {
        addConnectionAndNavigateToExplorer()

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Network Connections").assertIsDisplayed()
    }

    @Test
    fun smbExplorerNavigatesIntoDirectoryAndBack() {
        addConnectionAndNavigateToExplorer()

        composeTestRule.onNodeWithText("Alarms").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(NetworkConnectionFixtures.smbConnection.displayName)
            .assertIsDisplayed()
    }
}
