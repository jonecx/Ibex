package com.jonecx.ibex.ui.analysis

import app.cash.turbine.test
import com.jonecx.ibex.fixtures.FakeStorageAnalyzer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorageAnalysisViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeAnalyzer: FakeStorageAnalyzer
    private lateinit var viewModel: StorageAnalysisViewModel

    @Before
    fun setup() {
        fakeAnalyzer = FakeStorageAnalyzer()
        viewModel = StorageAnalysisViewModel(fakeAnalyzer, testDispatcher)
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
        val failingViewModel = StorageAnalysisViewModel(fakeAnalyzer, testDispatcher)

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
        val retryViewModel = StorageAnalysisViewModel(fakeAnalyzer, testDispatcher)

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
}
