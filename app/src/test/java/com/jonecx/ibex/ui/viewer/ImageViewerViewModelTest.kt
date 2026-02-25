package com.jonecx.ibex.ui.viewer

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.jonecx.ibex.fixtures.testImageFileItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageViewerViewModelTest {

    @Test
    fun `uiState reflects args files and index`() = runTest {
        val files = listOf(
            testImageFileItem("photo1.jpg"),
            testImageFileItem("photo2.jpg"),
            testImageFileItem("photo3.jpg"),
        )
        val args = ImageViewerArgs().apply { set(files, 1) }
        val viewModel = ImageViewerViewModel(args)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(files, state.viewableFiles)
            assertEquals(1, state.initialIndex)
        }
    }

    @Test
    fun `uiState has empty list when args not set`() = runTest {
        val args = ImageViewerArgs()
        val viewModel = ImageViewerViewModel(args)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.viewableFiles.isEmpty())
            assertEquals(0, state.initialIndex)
        }
    }

    @Test
    fun `onCleared clears the args`() {
        val files = listOf(testImageFileItem("photo.jpg"))
        val args = ImageViewerArgs().apply { set(files, 0) }
        val viewModel = ImageViewerViewModel(args)

        // Put ViewModel in a ViewModelStore and clear it to trigger onCleared
        val store = ViewModelStore()
        store.put("test", viewModel)
        store.clear()

        assertTrue(args.viewableFiles.isEmpty())
        assertEquals(0, args.initialIndex)
    }
}
