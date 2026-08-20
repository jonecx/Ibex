package com.jonecx.ibex.ui.settings

import app.cash.turbine.test
import com.jonecx.azmaree.player.model.PlayerSettings
import com.jonecx.ibex.fixtures.FakePlayerSettingsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakePreferences: FakePlayerSettingsPreferences
    private lateinit var viewModel: PlayerSettingsViewModel

    @Before
    fun setup() {
        // settings uses stateIn(viewModelScope), which dispatches on Main.
        Dispatchers.setMain(testDispatcher)
        fakePreferences = FakePlayerSettingsPreferences()
        viewModel = PlayerSettingsViewModel(fakePreferences, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // The loading null may be conflated away before collection starts, so drain to the first value.
    private suspend fun app.cash.turbine.ReceiveTurbine<PlayerSettings?>.awaitSettings() =
        awaitItem() ?: awaitItem()!!

    @Test
    fun `settings emits ibex defaults`() = runTest {
        viewModel.settings.test {
            val defaults = awaitSettings()
            assertEquals(10_000L, defaults.controls.seekStepMs)
            // Ibex ships cast visible.
            assertTrue(defaults.cast.enabled)
        }
    }

    @Test
    fun `update is reflected in settings`() = runTest {
        viewModel.settings.test {
            assertEquals(10_000L, awaitSettings().controls.seekStepMs)

            viewModel.update { it.copy(controls = it.controls.copy(seekStepMs = 30_000L)) }
            assertEquals(30_000L, awaitSettings().controls.seekStepMs)
        }
    }

    @Test
    fun `update writes through to preferences`() = runTest {
        viewModel.update {
            it.copy(controls = it.controls.copy(hdMinHeight = 1080))
        }

        assertEquals(1080, fakePreferences.currentSettings().controls.hdMinHeight)
    }

    @Test
    fun `reset restores defaults`() = runTest {
        viewModel.update { it.copy(controls = it.controls.copy(seekStepMs = 30_000L)) }
        assertEquals(30_000L, fakePreferences.currentSettings().controls.seekStepMs)

        viewModel.resetToDefaults()
        assertEquals(10_000L, fakePreferences.currentSettings().controls.seekStepMs)
    }
}
