package com.jonecx.ibex.ui.settings

import app.cash.turbine.test
import com.jonecx.ibex.data.model.ThemeMode
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import com.jonecx.ibex.fixtures.RecordingAnalytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakePreferences: FakeSettingsPreferences
    private lateinit var analytics: RecordingAnalytics
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        fakePreferences = FakeSettingsPreferences()
        analytics = RecordingAnalytics(RuntimeEnvironment.getApplication())
        viewModel = SettingsViewModel(fakePreferences, analytics.manager, testDispatcher)
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
    fun `initial state has network item count disabled`() = runTest {
        viewModel.uiState.test {
            assertFalse(awaitItem().networkFolderItemCountEnabled)
        }
    }

    @Test
    fun `uiState reflects network item count change to enabled`() = runTest {
        viewModel.uiState.test {
            assertFalse(awaitItem().networkFolderItemCountEnabled)

            fakePreferences.setNetworkFolderItemCountEnabled(true)
            assertTrue(awaitItem().networkFolderItemCountEnabled)
        }
    }

    @Test
    fun `setNetworkFolderItemCountEnabled updates preferences`() = runTest {
        viewModel.setNetworkFolderItemCountEnabled(true)
        assertTrue(fakePreferences.currentNetworkItemCountValue())

        viewModel.setNetworkFolderItemCountEnabled(false)
        assertFalse(fakePreferences.currentNetworkItemCountValue())
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

    @Test
    fun `initial state follows system theme`() = runTest {
        viewModel.uiState.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)
        }
    }

    @Test
    fun `uiState reflects theme mode change`() = runTest {
        viewModel.uiState.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)

            fakePreferences.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
        }
    }

    @Test
    fun `setThemeMode updates preferences`() = runTest {
        viewModel.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, fakePreferences.currentThemeMode())

        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, fakePreferences.currentThemeMode())
    }

    @Test
    fun `setThemeMode emits theme_change with from and to`() = runTest {
        viewModel.setThemeMode(ThemeMode.DARK)

        val props = analytics.event("theme_change")
        assertEquals("system", props?.get("from"))
        assertEquals("dark", props?.get("to"))
    }

    @Test
    fun `setThemeMode to same value emits nothing`() = runTest {
        viewModel.setThemeMode(ThemeMode.SYSTEM)

        assertFalse(analytics.eventNames().contains("theme_change"))
    }

    @Test
    fun `setViewMode emits view_mode_change`() = runTest {
        viewModel.setViewMode(ViewMode.GRID)

        val props = analytics.event("view_mode_change")
        assertEquals("list", props?.get("from"))
        assertEquals("grid", props?.get("to"))
    }

    @Test
    fun `setGridColumns emits grid_columns_change`() = runTest {
        viewModel.setGridColumns(6)

        val props = analytics.event("grid_columns_change")
        assertEquals(4, props?.get("from"))
        assertEquals(6, props?.get("to"))
    }

    @Test
    fun `enabling analytics emits consent change granted true`() = runTest {
        viewModel.setSendAnalyticsEnabled(true)

        assertEquals(true, analytics.event("analytics_consent_change")?.get("granted"))
    }

    @Test
    fun `disabling analytics emits consent change before revoking`() = runTest {
        viewModel.setSendAnalyticsEnabled(false)

        assertEquals(false, analytics.event("analytics_consent_change")?.get("granted"))
    }
}
