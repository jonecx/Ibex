package com.jonecx.ibex.analytics

import com.jonecx.ibex.fixtures.FakeAnalyticsProvider
import com.jonecx.ibex.fixtures.FakeAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerTest {

    private lateinit var fakeProvider: FakeAnalyticsProvider
    private lateinit var analyticsManager: AnalyticsManager

    @Before
    fun setup() {
        fakeProvider = FakeAnalyticsProvider()
        analyticsManager = AnalyticsManager(
            context = RuntimeEnvironment.getApplication(),
            analyticsProvider = fakeProvider,
            logger = FakeAppLogger(),
        )
    }

    @Test
    fun `initialize calls provider initialize`() {
        analyticsManager.initialize()

        assertTrue(fakeProvider.initialized)
    }

    @Test
    fun `initialize identifies user with persisted UUID`() {
        analyticsManager.initialize()

        assertNotNull(fakeProvider.identifiedUserId)
    }

    @Test
    fun `initialize reuses same userId across calls`() {
        analyticsManager.initialize()
        val firstUserId = fakeProvider.identifiedUserId

        fakeProvider.reset()
        analyticsManager.initialize()
        val secondUserId = fakeProvider.identifiedUserId

        assertEquals(firstUserId, secondUserId)
    }

    @Test
    fun `trackScreenView captures screen_view event with screen_name`() {
        analyticsManager.trackScreenView("home")

        assertEquals(1, fakeProvider.capturedEvents.size)
        val (event, props) = fakeProvider.capturedEvents.first()
        assertEquals("screen_view", event)
        assertEquals("home", props["screen_name"])
    }

    @Test
    fun `trackScreenView merges additional properties`() {
        analyticsManager.trackScreenView("home", mapOf("source" to "deep_link"))

        val (_, props) = fakeProvider.capturedEvents.first()
        assertEquals("home", props["screen_name"])
        assertEquals("deep_link", props["source"])
    }

    @Test
    fun `trackScreenExit captures screen_exit with duration`() {
        analyticsManager.trackScreenExit("home", 5000L)

        val (event, props) = fakeProvider.capturedEvents.first()
        assertEquals("screen_exit", event)
        assertEquals("home", props["screen_name"])
        assertEquals(5000L, props["duration_ms"])
        assertEquals(5.0, props["duration_seconds"])
    }

    @Test
    fun `trackTileClick captures tile_click with name and id`() {
        analyticsManager.trackTileClick("Storage", "storage_tile")

        val (event, props) = fakeProvider.capturedEvents.first()
        assertEquals("tile_click", event)
        assertEquals("Storage", props["tile_name"])
        assertEquals("storage_tile", props["tile_id"])
    }
}
