package com.jonecx.ibex.data.preferences

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.jonecx.azmaree.player.model.PlayerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// toPlayerSettings()/writeFrom() carry the storage <-> domain mapping, including the buffer clamps.
class PlayerSettingsMappingTest {

    @Test
    fun `empty preferences map to ibex defaults`() {
        val settings = emptyPreferences().toPlayerSettings()

        assertEquals(10_000L, settings.controls.seekStepMs)
        // The SDK ships cast off; the app ships it visible unless the user opts out.
        assertTrue(settings.cast.enabled)
    }

    @Test
    fun `min buffer is coerced above the playback thresholds`() {
        // 1s min is below the 5s after-rebuffer default, so it must clamp up to keep the load-control ordering.
        val prefs = mutablePreferencesOf(PlayerSettingsKeys.MIN_BUFFER_MS to 1_000)

        val buffer = prefs.toPlayerSettings().playback.buffer
        assertEquals(5_000, buffer.minBufferMs)
        assertTrue(buffer.maxBufferMs >= buffer.minBufferMs)
    }

    @Test
    fun `max buffer is coerced above min buffer`() {
        val prefs = mutablePreferencesOf(PlayerSettingsKeys.MAX_BUFFER_MS to 1_000)

        val buffer = prefs.toPlayerSettings().playback.buffer
        assertEquals(buffer.minBufferMs, buffer.maxBufferMs)
    }

    @Test
    fun `write then read round-trips edited values`() {
        val edited = PlayerSettings().let {
            it.copy(
                controls = it.controls.copy(seekStepMs = 30_000L, hdMinHeight = 1080),
                playback = it.playback.copy(maxVideoHeight = 720, maxPoolSize = 2),
                cast = it.cast.copy(enabled = false),
            )
        }

        val restored = mutablePreferencesOf().apply { writeFrom(edited) }.toPlayerSettings()

        assertEquals(30_000L, restored.controls.seekStepMs)
        assertEquals(1080, restored.controls.hdMinHeight)
        assertEquals(720, restored.playback.maxVideoHeight)
        assertEquals(2, restored.playback.maxPoolSize)
        assertEquals(false, restored.cast.enabled)
    }

    @Test
    fun `large controls style round-trips`() {
        val large = PlayerSettings().copy(style = PlayerSettingsPreferencesContract.LARGE_CONTROLS_STYLE)

        val restored = mutablePreferencesOf().apply { writeFrom(large) }.toPlayerSettings()

        assertEquals(PlayerSettingsPreferencesContract.LARGE_CONTROLS_STYLE, restored.style)
    }
}
