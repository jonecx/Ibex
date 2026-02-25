package com.jonecx.ibex.ui.explorer

import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.fixtures.FakeFileRepository
import com.jonecx.ibex.fixtures.FakeFileRepositoryFactory
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import com.jonecx.ibex.fixtures.testDirectoryFileItem
import com.jonecx.ibex.fixtures.testFileItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FileExplorerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeFileRepository
    private lateinit var fakeFactory: FakeFileRepositoryFactory
    private lateinit var fakePreferences: FakeSettingsPreferences

    private val storagePath = Environment.getExternalStorageDirectory().absolutePath
    private lateinit var viewModel: FileExplorerViewModel

    @Before
    fun setup() {
        fakeRepository = FakeFileRepository()
        fakeFactory = FakeFileRepositoryFactory(fakeRepository)
        fakePreferences = FakeSettingsPreferences()
        viewModel = createViewModel()
    }

    private fun createViewModel(
        sourceType: String = FileSourceType.LOCAL_STORAGE.name,
        rootPath: String = "",
        title: String = "",
    ): FileExplorerViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "sourceType" to sourceType,
                "rootPath" to rootPath,
                "title" to title,
            ),
        )
        return FileExplorerViewModel(fakeFactory, fakePreferences, savedStateHandle, testDispatcher)
    }

    private fun navigateToSubdir(name: String = "subdir") {
        val dir = testDirectoryFileItem(name, path = "$storagePath/$name")
        viewModel.navigateTo(dir)
    }

    @Test
    fun `initial state loads files`() = runTest {
        fakeRepository.filesToReturn = listOf(testFileItem("file1.txt"), testFileItem("file2.txt"))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.files.size)
        }
    }

    @Test
    fun `navigateTo directory updates navigation stack`() = runTest {
        navigateToSubdir()

        val state = viewModel.uiState.value
        assertEquals(2, state.navigationStack.size)
        assertTrue(state.navigationStack.last().endsWith("subdir"))
    }

    @Test
    fun `navigateTo file selects it`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            val file = testFileItem("photo.jpg")
            viewModel.navigateTo(file)

            assertEquals("photo.jpg", awaitItem().selectedFile?.name)
        }
    }

    @Test
    fun `navigateUp removes from stack and returns false at root`() = runTest {
        navigateToSubdir()

        assertTrue(viewModel.navigateUp())
        assertEquals(1, viewModel.uiState.value.navigationStack.size)

        assertFalse(viewModel.navigateUp())
    }

    @Test
    fun `selectFile updates and clears selectedFile`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.selectFile(testFileItem("doc.pdf"))
            assertEquals("doc.pdf", awaitItem().selectedFile?.name)

            viewModel.selectFile(null)
            assertNull(awaitItem().selectedFile)
        }
    }

    @Test
    fun `canNavigateUp reflects navigation state`() = runTest {
        assertFalse(viewModel.canNavigateUp())

        navigateToSubdir()
        assertTrue(viewModel.canNavigateUp())
    }

    @Test
    fun `getCurrentDirectoryName reflects navigation state`() = runTest {
        assertNull(viewModel.getCurrentDirectoryName())

        navigateToSubdir("Documents")
        assertEquals("Documents", viewModel.getCurrentDirectoryName())
    }

    @Test
    fun `loadFiles error sets error state`() = runTest {
        val error = RuntimeException("disk error")
        fakeRepository.errorToThrow = error
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(error, state.error)
        }
    }

    @Test
    fun `setTitle updates currentPath`() = runTest {
        viewModel.setTitle("My Custom Title")
        assertEquals("My Custom Title", viewModel.uiState.value.currentPath)
    }

    @Test
    fun `non-folder-navigation source disables folder navigation`() = runTest {
        viewModel = createViewModel(sourceType = FileSourceType.LOCAL_IMAGES.name, title = "Images")

        val state = viewModel.uiState.value
        assertFalse(state.allowFolderNavigation)
    }

    @Test
    fun `navigateTo directory is no-op when folder navigation disabled`() = runTest {
        viewModel = createViewModel(sourceType = FileSourceType.LOCAL_IMAGES.name, title = "Images")

        val stackBefore = viewModel.uiState.value.navigationStack.size
        val dir = testDirectoryFileItem("subdir")
        viewModel.navigateTo(dir)

        assertEquals(stackBefore, viewModel.uiState.value.navigationStack.size)
        assertEquals(dir, viewModel.uiState.value.selectedFile)
    }

    @Test
    fun `getCurrentDirectoryName returns currentPath when folder navigation disabled`() = runTest {
        viewModel = createViewModel(sourceType = FileSourceType.LOCAL_AUDIO.name, title = "Audio")
        assertEquals("Audio", viewModel.getCurrentDirectoryName())
    }

    @Test
    fun `navigateUp clears selectedFile`() = runTest {
        navigateToSubdir()
        viewModel.selectFile(testFileItem("file.txt"))
        assertEquals("file.txt", viewModel.uiState.value.selectedFile?.name)

        viewModel.navigateUp()
        assertNull(viewModel.uiState.value.selectedFile)
    }

    @Test
    fun `initial viewMode is LIST`() = runTest {
        assertEquals(ViewMode.LIST, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `viewMode reflects preference change to GRID`() = runTest {
        viewModel.uiState.test {
            assertEquals(ViewMode.LIST, awaitItem().viewMode)

            fakePreferences.setViewMode(ViewMode.GRID)
            assertEquals(ViewMode.GRID, awaitItem().viewMode)
        }
    }

    @Test
    fun `viewMode reflects preference change back to LIST`() = runTest {
        viewModel.uiState.test {
            assertEquals(ViewMode.LIST, awaitItem().viewMode)

            fakePreferences.setViewMode(ViewMode.GRID)
            assertEquals(ViewMode.GRID, awaitItem().viewMode)

            fakePreferences.setViewMode(ViewMode.LIST)
            assertEquals(ViewMode.LIST, awaitItem().viewMode)
        }
    }
}
