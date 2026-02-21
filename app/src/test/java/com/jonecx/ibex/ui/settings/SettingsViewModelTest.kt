package com.jonecx.ibex.ui.settings

import app.cash.turbine.test
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
        assertTrue(fakePreferences.currentValue())

        viewModel.setSendAnalyticsEnabled(false)
        assertFalse(fakePreferences.currentValue())
    }
}

class FakeSettingsPreferences : SettingsPreferencesContract {
    private val _sendAnalyticsEnabled = MutableStateFlow(false)
    override val sendAnalyticsEnabled: Flow<Boolean> = _sendAnalyticsEnabled

    override suspend fun setSendAnalyticsEnabled(enabled: Boolean) {
        _sendAnalyticsEnabled.value = enabled
    }

    fun currentValue(): Boolean = _sendAnalyticsEnabled.value
}
