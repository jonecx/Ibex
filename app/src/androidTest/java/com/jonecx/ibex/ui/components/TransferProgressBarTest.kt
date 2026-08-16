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
import com.jonecx.ibex.fixtures.sheetTransferSnapshot
import org.junit.Assert.assertEquals
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

        composeTestRule.onNodeWithText("Moving 5 files").assertIsDisplayed()
    }

    @Test
    fun tappingBarUnfurlsDetailWithCurrentFileAndQueuedJob() {
        fakeManager.setSnapshot(sheetTransferSnapshot())
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Moving 45 files").performClick()
        composeTestRule.waitForIdle()

        // The panel unfurls inline: current file, per-job controls, and the queued job below.
        composeTestRule.onNodeWithText("IMG_2043.mp4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause all").assertIsDisplayed()
        composeTestRule.onNodeWithText("Queued", substring = true).assertIsDisplayed()
    }

    @Test
    fun perJobPauseAndCancelInvokeTheManager() {
        fakeManager.setSnapshot(sheetTransferSnapshot())
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Moving 45 files").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Pause").performClick()
        assertTrue("job-1" in fakeManager.pausedIds)

        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue("job-1" in fakeManager.cancelledIds)
    }

    @Test
    fun pauseAllInvokesTheManager() {
        fakeManager.setSnapshot(sheetTransferSnapshot())
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Moving 45 files").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Pause all").performClick()
        assertEquals(1, fakeManager.pauseAllCount)
    }

    @Test
    fun cancellingQueuedJobFromItsCardInvokesTheManager() {
        fakeManager.setSnapshot(sheetTransferSnapshot())
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Moving 45 files").performClick()
        composeTestRule.waitForIdle()

        // The queued card carries the "Cancel transfer" icon button.
        composeTestRule.onNodeWithContentDescription("Cancel transfer").performClick()
        assertTrue("job-2" in fakeManager.cancelledIds)
    }
}
