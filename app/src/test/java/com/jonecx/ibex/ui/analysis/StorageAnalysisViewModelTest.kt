package com.jonecx.ibex.ui.analysis

import app.cash.turbine.test
import com.jonecx.ibex.fixtures.FakeStorageAnalyzer
import com.jonecx.ibex.fixtures.RecordingAnalytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StorageAnalysisViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeAnalyzer: FakeStorageAnalyzer
    private lateinit var analytics: RecordingAnalytics
    private lateinit var viewModel: StorageAnalysisViewModel

    @Before
    fun setup() {
        fakeAnalyzer = FakeStorageAnalyzer()
        analytics = RecordingAnalytics(RuntimeEnvironment.getApplication())
        viewModel = StorageAnalysisViewModel(fakeAnalyzer, analytics.manager, testDispatcher)
    }

    @Test
    fun `initial state loads breakdown successfully`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertNotNull(state.breakdown)
        }
    }

    @Test
    fun `breakdown contains correct total bytes`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(64_000_000_000L, state.breakdown?.totalBytes)
        }
    }

    @Test
    fun `breakdown contains correct used bytes`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(40_000_000_000L, state.breakdown?.usedBytes)
        }
    }

    @Test
    fun `breakdown contains six categories`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(6, state.breakdown?.categories?.size)
        }
    }

    @Test
    fun `error state when analyzer fails`() = runTest {
        fakeAnalyzer.shouldFail = true
        val failingViewModel = StorageAnalysisViewModel(fakeAnalyzer, analytics.manager, testDispatcher)

        failingViewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertNull(state.breakdown)
        }
    }

    @Test
    fun `retry after failure loads successfully`() = runTest {
        fakeAnalyzer.shouldFail = true
        val retryViewModel = StorageAnalysisViewModel(fakeAnalyzer, analytics.manager, testDispatcher)

        retryViewModel.uiState.test {
            val errorState = awaitItem()
            assertNotNull(errorState.error)

            fakeAnalyzer.shouldFail = false
            retryViewModel.analyze()

            // Skip intermediate loading state emitted before the coroutine completes
            skipItems(1)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertNull(successState.error)
            assertNotNull(successState.breakdown)
        }
    }

    @Test
    fun `initial analysis emits start and complete events`() = runTest {
        // viewModel from setup() already ran the initial analysis.
        assertEquals("initial", analytics.event("storage_analysis_start")?.get("trigger"))

        val complete = analytics.event("storage_analysis_complete")
        assertEquals("success", complete?.get("result"))
        assertEquals(64_000_000_000L, complete?.get("total_bytes"))
        assertEquals(40_000_000_000L, complete?.get("used_bytes"))
        assertEquals(6, complete?.get("category_count"))
        // Latency also flows to Axiom as a QoE metric.
        assertEquals("success", analytics.metric("storage_analysis")?.get("result"))
    }

    @Test
    fun `retry emits start with retry trigger`() = runTest {
        viewModel.analyze()

        assertEquals("retry", analytics.event("storage_analysis_start")?.get("trigger"))
    }

    @Test
    fun `failed analysis emits complete with failure result and error code`() = runTest {
        fakeAnalyzer.shouldFail = true
        StorageAnalysisViewModel(fakeAnalyzer, analytics.manager, testDispatcher)

        val complete = analytics.event("storage_analysis_complete")
        assertEquals("failure", complete?.get("result"))
        assertNotNull(complete?.get("error_code"))
    }
}
