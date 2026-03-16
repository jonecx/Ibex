package com.jonecx.ibex.ui.analysis

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.fixtures.FakeStorageAnalyzer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class StorageAnalysisScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var fakeAnalyzer: FakeStorageAnalyzer

    @Before
    fun setup() {
        hiltRule.inject()
        fakeAnalyzer.reset()
    }

    private fun navigateToAnalysis() {
        composeTestRule.onNodeWithText("Analysis").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun displaysTitle() {
        navigateToAnalysis()

        composeTestRule.onNodeWithText("Storage Analysis").assertIsDisplayed()
    }

    @Test
    fun successStateShowsTotalStorage() {
        navigateToAnalysis()

        composeTestRule.onNodeWithText("Total Storage").assertIsDisplayed()
    }

    @Test
    fun successStateShowsUsedAndFree() {
        navigateToAnalysis()

        composeTestRule.onNodeWithText("Used:", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Free:", substring = true).assertIsDisplayed()
    }

    @Test
    fun successStateShowsCategoryLabels() {
        navigateToAnalysis()

        composeTestRule.onNodeWithText("Images", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Videos", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Documents", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Apps", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Other", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Free Space", substring = true).assertIsDisplayed()
    }

    @Test
    fun errorStateShowsRetryButton() {
        fakeAnalyzer.shouldFail = true
        navigateToAnalysis()

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun retryAfterErrorShowsSuccess() {
        fakeAnalyzer.shouldFail = true
        navigateToAnalysis()

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()

        fakeAnalyzer.shouldFail = false
        composeTestRule.onNodeWithText("Retry").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Total Storage").assertIsDisplayed()
    }

    @Test
    fun backButtonReturnsToHome() {
        navigateToAnalysis()

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Ibex").assertIsDisplayed()
    }
}
