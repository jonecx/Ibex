package com.jonecx.ibex.ui.live

import app.cash.turbine.test
import com.jonecx.ibex.data.model.VideoFeed
import com.jonecx.ibex.fixtures.FakeLiveStreamsPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveFeedViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakePreferences: FakeLiveStreamsPreferences

    @Before
    fun setup() {
        fakePreferences = FakeLiveStreamsPreferences()
    }

    private fun createViewModel() = LiveFeedViewModel(fakePreferences, testDispatcher)

    @Test
    fun `initial state has no streams`() = runTest {
        createViewModel().uiState.test {
            assertTrue(awaitItem().streams.isEmpty())
        }
    }

    @Test
    fun `addStream updates state`() = runTest {
        val viewModel = createViewModel()
        val stream = VideoFeed(id = "1", title = "Live", url = "https://example.com/s.m3u8")

        viewModel.uiState.test {
            assertTrue(awaitItem().streams.isEmpty())
            viewModel.addStream(stream)
            val state = awaitItem()
            assertEquals(1, state.streams.size)
            assertEquals(stream, state.streams.first())
        }
    }

    @Test
    fun `removeStream removes only the target`() = runTest {
        val viewModel = createViewModel()
        val keep = VideoFeed(id = "keep", title = "A", url = "https://example.com/a.m3u8")
        val remove = VideoFeed(id = "remove", title = "B", url = "https://example.com/b.m3u8")

        viewModel.uiState.test {
            assertTrue(awaitItem().streams.isEmpty())
            viewModel.addStream(keep)
            assertEquals(1, awaitItem().streams.size)
            viewModel.addStream(remove)
            assertEquals(2, awaitItem().streams.size)

            viewModel.removeStream(remove.id)
            val state = awaitItem()
            assertEquals(1, state.streams.size)
            assertEquals(keep, state.streams.first())
        }
    }

    @Test
    fun `updateStream replaces matching stream`() = runTest {
        val viewModel = createViewModel()
        val original = VideoFeed(id = "1", title = "Old", url = "https://example.com/s.m3u8")
        val updated = original.copy(title = "New")

        viewModel.uiState.test {
            assertTrue(awaitItem().streams.isEmpty())
            viewModel.addStream(original)
            assertEquals("Old", awaitItem().streams.first().title)

            viewModel.updateStream(updated)
            assertEquals("New", awaitItem().streams.first().title)
        }
    }

    @Test
    fun `setStreamToEdit and clear update edit state`() = runTest {
        val viewModel = createViewModel()
        val stream = VideoFeed(id = "1", title = "Live", url = "https://example.com/s.m3u8")

        viewModel.uiState.test {
            assertNull(awaitItem().streamToEdit)
            viewModel.setStreamToEdit(stream)
            assertEquals(stream, awaitItem().streamToEdit)
            viewModel.clearStreamToEdit()
            assertNull(awaitItem().streamToEdit)
        }
    }
}
