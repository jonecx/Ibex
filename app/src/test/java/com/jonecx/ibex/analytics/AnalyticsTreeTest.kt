package com.jonecx.ibex.analytics

import android.util.Log
import com.jonecx.ibex.fixtures.FakeAnalyticsProvider
import com.jonecx.ibex.fixtures.FakeAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AnalyticsTreeTest {

    private lateinit var fakeProvider: FakeAnalyticsProvider
    private lateinit var analyticsTree: AnalyticsTree

    @Before
    fun setup() {
        fakeProvider = FakeAnalyticsProvider()
        val analyticsManager = AnalyticsManager(
            context = RuntimeEnvironment.getApplication(),
            analyticsProvider = fakeProvider,
            logger = FakeAppLogger(),
        )
        analyticsTree = AnalyticsTree(analyticsManager)
    }

    @Test
    fun `log captures error events`() {
        analyticsTree.log(Log.ERROR, "TestTag", "Something failed", null)

        assertEquals(1, fakeProvider.capturedEvents.size)
        val (event, props) = fakeProvider.capturedEvents.first()
        assertEquals("log_error", event)
        assertEquals("TestTag", props["tag"])
        assertEquals("Something failed", props["message"])
    }

    @Test
    fun `log captures warning events`() {
        analyticsTree.log(Log.WARN, "TestTag", "Something suspicious", null)

        assertEquals(1, fakeProvider.capturedEvents.size)
        val (event, _) = fakeProvider.capturedEvents.first()
        assertEquals("log_warning", event)
    }

    @Test
    fun `log ignores debug priority`() {
        analyticsTree.log(Log.DEBUG, "TestTag", "Debug message", null)

        assertTrue(fakeProvider.capturedEvents.isEmpty())
    }

    @Test
    fun `log ignores info priority`() {
        analyticsTree.log(Log.INFO, "TestTag", "Info message", null)

        assertTrue(fakeProvider.capturedEvents.isEmpty())
    }

    @Test
    fun `log ignores verbose priority`() {
        analyticsTree.log(Log.VERBOSE, "TestTag", "Verbose message", null)

        assertTrue(fakeProvider.capturedEvents.isEmpty())
    }

    @Test
    fun `log includes exception details when throwable is present`() {
        val exception = IllegalStateException("bad state")

        analyticsTree.log(Log.ERROR, "TestTag", "Crash", exception)

        val (_, props) = fakeProvider.capturedEvents.first()
        assertEquals("IllegalStateException", props["exception"])
        assertTrue((props["stacktrace"] as String).contains("bad state"))
    }

    @Test
    fun `log uses unknown tag when tag is null`() {
        analyticsTree.log(Log.ERROR, null, "No tag", null)

        val (_, props) = fakeProvider.capturedEvents.first()
        assertEquals("unknown", props["tag"])
    }

    @Test
    fun `log truncates stacktrace to 1000 characters`() {
        val exception = RuntimeException("x".repeat(2000))

        analyticsTree.log(Log.ERROR, "TestTag", "Long error", exception)

        val (_, props) = fakeProvider.capturedEvents.first()
        assertTrue((props["stacktrace"] as String).length <= 1000)
    }
}
