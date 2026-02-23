package com.jonecx.ibex.ui.settings

import app.cash.turbine.test
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakePreferences: FakeSettingsPreferences
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        fakePreferences = FakeSettingsPreferences()
        viewModel = SettingsViewModel(fakePreferences, testDispatcher)
    }

    @Test
    fun `initial state has analytics disabled`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.sendAnalyticsEnabled)
        }
    }

    @Test
    fun `uiState reflects preferences change to enabled`() = runTest {
        viewModel.uiState.test {
            assertFalse(awaitItem().sendAnalyticsEnabled)

            fakePreferences.setSendAnalyticsEnabled(true)
            assertTrue(awaitItem().sendAnalyticsEnabled)
        }
    }

    @Test
    fun `uiState reflects preferences change to disabled`() = runTest {
        viewModel.uiState.test {
            assertFalse(awaitItem().sendAnalyticsEnabled)

            fakePreferences.setSendAnalyticsEnabled(true)
            assertTrue(awaitItem().sendAnalyticsEnabled)

            fakePreferences.setSendAnalyticsEnabled(false)
            assertFalse(awaitItem().sendAnalyticsEnabled)
        }
    }

    @Test
    fun `setSendAnalyticsEnabled updates preferences`() = runTest {
        viewModel.setSendAnalyticsEnabled(true)
        assertTrue(fakePreferences.currentAnalyticsValue())

        viewModel.setSendAnalyticsEnabled(false)
        assertFalse(fakePreferences.currentAnalyticsValue())
    }

    @Test
    fun `initial state has list view mode`() = runTest {
        viewModel.uiState.test {
            assertEquals(ViewMode.LIST, awaitItem().viewMode)
        }
    }

    @Test
    fun `uiState reflects view mode change to grid`() = runTest {
        viewModel.uiState.test {
            assertEquals(ViewMode.LIST, awaitItem().viewMode)

            fakePreferences.setViewMode(ViewMode.GRID)
            assertEquals(ViewMode.GRID, awaitItem().viewMode)
        }
    }

    @Test
    fun `setViewMode updates preferences`() = runTest {
        viewModel.setViewMode(ViewMode.GRID)
        assertEquals(ViewMode.GRID, fakePreferences.currentViewMode())

        viewModel.setViewMode(ViewMode.LIST)
        assertEquals(ViewMode.LIST, fakePreferences.currentViewMode())
    }
}
