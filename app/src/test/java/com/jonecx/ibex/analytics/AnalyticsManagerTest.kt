package com.jonecx.ibex.analytics

import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.data.model.NetworkProtocol
import com.jonecx.ibex.data.model.ThemeMode
import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.fixtures.FakeAnalyticsProvider
import com.jonecx.ibex.fixtures.FakeAppLogger
import com.jonecx.ibex.fixtures.FakeCrashReporter
import com.jonecx.ibex.fixtures.FakeMetricsProvider
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerTest {

    private lateinit var fakeProvider: FakeAnalyticsProvider
    private lateinit var fakeMetrics: FakeMetricsProvider
    private lateinit var analyticsManager: AnalyticsManager

    @Before
    fun setup() {
        fakeProvider = FakeAnalyticsProvider()
        fakeMetrics = FakeMetricsProvider()
        analyticsManager = AnalyticsManager(
            context = RuntimeEnvironment.getApplication(),
            analytics = fakeProvider,
            metrics = fakeMetrics,
            crashReporter = FakeCrashReporter(),
            settingsPreferences = FakeSettingsPreferences(),
            scope = CoroutineScope(Dispatchers.Unconfined),
            logger = FakeAppLogger(),
        )
    }

    private fun lastEvent(name: String) = fakeProvider.capturedEvents.last { it.first == name }.second

    private fun lastMetric(name: String) = fakeMetrics.trackedEvents.last { it.first == name }.second

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

    @Test
    fun `enum properties are lowercased on the wire`() {
        analyticsManager.trackTileClick("SMB", "smb_tile", FileSourceType.LOCAL_STORAGE)

        assertEquals("local_storage", lastEvent("tile_click")["source_type"])
    }

    @Test
    fun `trackFileDelete records disposition and result`() {
        analyticsManager.trackFileDelete(
            sourceType = FileSourceType.SMB,
            isRemote = true,
            itemCount = 3,
            permanent = true,
            success = true,
            durationMs = 12L,
        )

        val props = lastEvent("file_delete")
        assertEquals("permanent", props["disposition"])
        assertEquals("success", props["result"])
        assertEquals(3, props["item_count"])
        assertEquals("smb", props["source_type"])
    }

    @Test
    fun `local delete disposition is trash`() {
        analyticsManager.trackFileDelete(FileSourceType.LOCAL_STORAGE, false, 1, permanent = false, success = false, durationMs = 1L)

        val props = lastEvent("file_delete")
        assertEquals("trash", props["disposition"])
        assertEquals("failure", props["result"])
    }

    @Test
    fun `trackPaste maps copy and move to distinct events`() {
        analyticsManager.trackPaste(ClipboardOperation.COPY, FileSourceType.LOCAL_STORAGE, false, 2, 100L, true, 5L)
        analyticsManager.trackPaste(ClipboardOperation.MOVE, FileSourceType.LOCAL_STORAGE, false, 2, 100L, true, 5L)

        assertTrue(fakeProvider.capturedEvents.any { it.first == "file_copy" })
        assertTrue(fakeProvider.capturedEvents.any { it.first == "file_move" })
    }

    @Test
    fun `remote paste also emits file_transfer QoE metric`() {
        analyticsManager.trackPaste(ClipboardOperation.COPY, FileSourceType.SMB, true, 1, 2048L, true, 30L)

        val metric = lastMetric("file_transfer")
        assertEquals(2048L, metric["size_bytes"])
        assertEquals("success", metric["result"])
    }

    @Test
    fun `local paste does not emit file_transfer metric`() {
        analyticsManager.trackPaste(ClipboardOperation.COPY, FileSourceType.LOCAL_STORAGE, false, 1, 2048L, true, 30L)

        assertTrue(fakeMetrics.trackedEvents.none { it.first == "file_transfer" })
    }

    @Test
    fun `trackConnectionConnect fans to behavioral event and latency metric`() {
        analyticsManager.trackConnectionConnect(NetworkProtocol.SMB, success = true, durationMs = 42L)

        assertEquals("smb", lastEvent("connection_connect")["protocol"])
        assertEquals(42L, lastMetric("connection_latency")["duration_ms"])
    }

    @Test
    fun `trackContentLoad routes to metrics not behavioral events`() {
        analyticsManager.trackContentLoad(FileSourceType.SMB, true, itemCount = 10, durationMs = 8L, success = true)

        assertTrue(fakeProvider.capturedEvents.none { it.first == "content_load" })
        assertEquals(10, lastMetric("content_load")["item_count"])
    }

    @Test
    fun `trackAppStart routes to metrics`() {
        analyticsManager.trackAppStart(123L)

        assertEquals(123L, lastMetric("app_start")["duration_ms"])
        assertTrue(fakeProvider.capturedEvents.none { it.first == "app_start" })
    }

    @Test
    fun `trackThemeChange records lowercased from and to`() {
        analyticsManager.trackThemeChange(ThemeMode.SYSTEM, ThemeMode.DARK)

        val props = lastEvent("theme_change")
        assertEquals("system", props["from"])
        assertEquals("dark", props["to"])
    }

    @Test
    fun `null properties are dropped`() {
        analyticsManager.trackConnectionDeleted(null)

        assertFalse(lastEvent("connection_delete").containsKey("protocol"))
    }

    @Test
    fun `trackMediaViewerPage records direction`() {
        analyticsManager.trackMediaViewerPage(FileType.VIDEO, forward = false, pageIndex = 2)

        val props = lastEvent("media_viewer_page")
        assertEquals("prev", props["direction"])
        assertEquals("video", props["media_type"])
    }
}
