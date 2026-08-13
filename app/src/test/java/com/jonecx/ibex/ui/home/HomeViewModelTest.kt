package com.jonecx.ibex.ui.home

import app.cash.turbine.test
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.fixtures.FakeHomeSourceStatsRepository
import com.jonecx.ibex.fixtures.HomeStatsFixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads sources and storage usage from repository`() = runTest {
        val viewModel = HomeViewModel(FakeHomeSourceStatsRepository(), testDispatcher)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(HomeStatsFixtures.sample.sources, state.stats)
            assertEquals(HomeStatsFixtures.sample.storageUsage, state.storageUsage)
        }
    }

    @Test
    fun `stats expose count for every local file tile`() = runTest {
        val viewModel = HomeViewModel(FakeHomeSourceStatsRepository(), testDispatcher)

        viewModel.uiState.test {
            val stats = awaitItem().stats
            assertEquals(1056, stats[FileSourceType.LOCAL_VIDEOS]?.count)
            assertEquals(364, stats[FileSourceType.LOCAL_DOWNLOADS]?.count)
            assertEquals(172, stats[FileSourceType.LOCAL_APPS]?.count)
        }
    }

    @Test
    fun `stats stay empty when repository fails`() = runTest {
        val repository = FakeHomeSourceStatsRepository().apply { shouldFail = true }
        val viewModel = HomeViewModel(repository, testDispatcher)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.stats.isEmpty())
            assertNull(state.storageUsage)
        }
    }
}
