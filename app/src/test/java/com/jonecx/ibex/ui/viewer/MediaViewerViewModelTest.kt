package com.jonecx.ibex.ui.viewer

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.jonecx.ibex.fixtures.FakeFileTrashManager
import com.jonecx.ibex.fixtures.FakePlayerFactory
import com.jonecx.ibex.fixtures.testImageFileItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaViewerViewModelTest {

    private val playerFactory = FakePlayerFactory()
    private val trashManager = FakeFileTrashManager()

    private fun createViewModel(
        files: List<com.jonecx.ibex.data.model.FileItem> = emptyList(),
        index: Int = 0,
    ): MediaViewerViewModel {
        val args = MediaViewerArgs().apply { if (files.isNotEmpty()) set(files, index) }
        return MediaViewerViewModel(args, playerFactory, trashManager)
    }

    @Test
    fun `uiState reflects args files and index`() = runTest {
        val files = listOf(
            testImageFileItem("photo1.jpg"),
            testImageFileItem("photo2.jpg"),
            testImageFileItem("photo3.jpg"),
        )
        val viewModel = createViewModel(files, 1)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(files, state.viewableFiles)
            assertEquals(1, state.initialIndex)
        }
    }

    @Test
    fun `uiState has empty list when args not set`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.viewableFiles.isEmpty())
            assertEquals(0, state.initialIndex)
        }
    }

    @Test
    fun `onCleared clears the args`() {
        val files = listOf(testImageFileItem("photo.jpg"))
        val args = MediaViewerArgs().apply { set(files, 0) }
        val viewModel = MediaViewerViewModel(args, playerFactory, trashManager)

        val store = ViewModelStore()
        store.put("test", viewModel)
        store.clear()

        assertTrue(args.viewableFiles.isEmpty())
        assertEquals(0, args.initialIndex)
    }

    @Test
    fun `playerFactory is the injected instance`() {
        val viewModel = createViewModel()
        assertSame(playerFactory, viewModel.playerFactory)
    }

    @Test
    fun `deleteFile removes file from state`() = runTest {
        val files = listOf(
            testImageFileItem("photo1.jpg"),
            testImageFileItem("photo2.jpg"),
        )
        val viewModel = createViewModel(files)

        viewModel.uiState.test {
            assertEquals(2, awaitItem().viewableFiles.size)

            viewModel.deleteFile(files[0])

            val updated = awaitItem()
            assertEquals(1, updated.viewableFiles.size)
            assertEquals("photo2.jpg", updated.viewableFiles[0].name)
        }
    }

    @Test
    fun `deleteFile calls trashManager`() = runTest {
        val files = listOf(testImageFileItem("photo.jpg"))
        val viewModel = createViewModel(files)

        viewModel.deleteFile(files[0])

        // Give coroutine time to complete
        viewModel.uiState.test {
            awaitItem() // final state
        }
        assertEquals(1, trashManager.trashedFiles.size)
        assertEquals("photo.jpg", trashManager.trashedFiles[0].name)
    }

    @Test
    fun `deleteFile does not remove on failure`() = runTest {
        val files = listOf(testImageFileItem("photo.jpg"))
        trashManager.shouldSucceed = false
        val viewModel = createViewModel(files)

        viewModel.uiState.test {
            assertEquals(1, awaitItem().viewableFiles.size)

            viewModel.deleteFile(files[0])

            // No state change expected — use expectNoEvents or a short timeout
            expectNoEvents()
        }
    }
}
