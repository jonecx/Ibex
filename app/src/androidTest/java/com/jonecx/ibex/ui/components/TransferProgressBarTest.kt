package com.jonecx.ibex.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.data.transfer.TransferManager
import com.jonecx.ibex.fixtures.FakeTransferManager
import com.jonecx.ibex.fixtures.runningTransferSnapshot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

class TransferProgressBarTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val fakeManager: FakeTransferManager
        get() = GlobalContext.get().get<TransferManager>() as FakeTransferManager

    @Test
    fun showsRunningTransferUnderTheToolbar() {
        fakeManager.setSnapshot(runningTransferSnapshot())
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Moving 3 files").assertIsDisplayed()
    }

    @Test
    fun tappingBarExpandsToCurrentFileAndCancels() {
        fakeManager.setSnapshot(runningTransferSnapshot(currentFileName = "IMG_2043.mp4"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Moving 3 files").performClick()
        composeTestRule.onNodeWithText("IMG_2043.mp4").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Cancel transfer").performClick()
        assertTrue("job-1" in fakeManager.cancelledIds)
    }
}
