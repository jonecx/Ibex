package com.jonecx.ibex.ui.analysis

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonecx.ibex.fixtures.FakeStorageAnalyzer
import com.jonecx.ibex.ui.components.PieChart
import com.jonecx.ibex.ui.components.PieChartSegment
import com.jonecx.ibex.ui.theme.GrayDark
import com.jonecx.ibex.ui.theme.SourceImagesColor
import com.jonecx.ibex.util.setIbexContent
import org.junit.Rule
import org.junit.Test

class StorageAnalysisScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleBreakdown = FakeStorageAnalyzer().let { analyzer ->
        kotlinx.coroutines.runBlocking { analyzer.analyze() }
    }

    @Test
    fun testLoadingStateShowsProgress() {
        composeTestRule.setIbexContent {
            StorageAnalysisScreenContent(
                uiState = StorageAnalysisUiState(isLoading = true),
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Storage Analysis").assertIsDisplayed()
    }

    @Test
    fun testSuccessStateShowsTotalStorage() {
        composeTestRule.setIbexContent {
            StorageAnalysisScreenContent(
                uiState = StorageAnalysisUiState(isLoading = false, breakdown = sampleBreakdown),
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Total Storage").assertIsDisplayed()
    }

    @Test
    fun testSuccessStateShowsUsedAndFree() {
        composeTestRule.setIbexContent {
            StorageAnalysisScreenContent(
                uiState = StorageAnalysisUiState(isLoading = false, breakdown = sampleBreakdown),
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Used:", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Free:", substring = true).assertIsDisplayed()
    }

    @Test
    fun testSuccessStateShowsCategoryLabels() {
        composeTestRule.setIbexContent {
            StorageAnalysisScreenContent(
                uiState = StorageAnalysisUiState(isLoading = false, breakdown = sampleBreakdown),
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Images", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Videos", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Documents", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Apps", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Other", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Free Space", substring = true).assertIsDisplayed()
    }

    @Test
    fun testErrorStateShowsRetryButton() {
        composeTestRule.setIbexContent {
            StorageAnalysisScreenContent(
                uiState = StorageAnalysisUiState(isLoading = false, error = RuntimeException("Test error")),
                onNavigateBack = {},
                onRetry = {},
            )
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun testErrorRetryCallsCallback() {
        var retryCalled = false

        composeTestRule.setIbexContent {
            StorageAnalysisScreenContent(
                uiState = StorageAnalysisUiState(isLoading = false, error = RuntimeException("Test error")),
                onNavigateBack = {},
                onRetry = { retryCalled = true },
            )
        }

        composeTestRule.onNodeWithText("Retry").performClick()
        assert(retryCalled) { "Expected onRetry to be called" }
    }

    @Test
    fun testBackButtonTriggersNavigation() {
        var navigatedBack = false

        composeTestRule.setIbexContent {
            StorageAnalysisScreenContent(
                uiState = StorageAnalysisUiState(isLoading = false, breakdown = sampleBreakdown),
                onNavigateBack = { navigatedBack = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        assert(navigatedBack) { "Expected onNavigateBack to be called" }
    }

    @Test
    fun testPieChartRendersSegments() {
        composeTestRule.setIbexContent {
            PieChart(
                segments = listOf(
                    PieChartSegment("Images (9.3 GB)", 10f, SourceImagesColor),
                    PieChartSegment("Free Space (22.4 GB)", 24f, GrayDark),
                ),
            )
        }

        composeTestRule.onNodeWithText("Images (9.3 GB)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Free Space (22.4 GB)").assertIsDisplayed()
    }
}
