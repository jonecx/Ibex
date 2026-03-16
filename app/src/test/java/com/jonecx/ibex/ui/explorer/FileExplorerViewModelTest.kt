package com.jonecx.ibex.ui.explorer

import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import org.robolectric.RuntimeEnvironment
import app.cash.turbine.test
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.data.repository.SmbContextProvider
import com.jonecx.ibex.fixtures.FakeFileClipboardManager
import com.jonecx.ibex.fixtures.FakeFileMoveManager
import com.jonecx.ibex.fixtures.FakeFileRepository
import com.jonecx.ibex.fixtures.FakeFileRepositoryFactory
import com.jonecx.ibex.fixtures.FakeFileTrashManager
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import com.jonecx.ibex.fixtures.testDirectoryFileItem
import com.jonecx.ibex.fixtures.testFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
    private lateinit var fakeTrashManager: FakeFileTrashManager
    private lateinit var fakeMoveManager: FakeFileMoveManager
    private lateinit var fakeClipboardManager: FakeFileClipboardManager

    private lateinit var storagePath: String
    private lateinit var viewModel: FileExplorerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        storagePath = Environment.getExternalStorageDirectory().absolutePath
        fakeRepository = FakeFileRepository()
        fakeFactory = FakeFileRepositoryFactory(fakeRepository)
        fakePreferences = FakeSettingsPreferences()
        fakeTrashManager = FakeFileTrashManager()
        fakeMoveManager = FakeFileMoveManager()
        fakeClipboardManager = FakeFileClipboardManager(fakeMoveManager)
        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        sourceType: String = FileSourceType.LOCAL_STORAGE.name,
        rootPath: String = "",
        title: String = "",
    ): FileExplorerViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                FileExplorerViewModel.ARG_SOURCE_TYPE to sourceType,
                FileExplorerViewModel.ARG_ROOT_PATH to rootPath,
                FileExplorerViewModel.ARG_TITLE to title,
            ),
        )
        return FileExplorerViewModel(
            fakeFactory,
            fakePreferences,
            fakeTrashManager,
            fakeMoveManager,
            fakeClipboardManager,
            SmbContextProvider(),
            RuntimeEnvironment.getApplication(),
            savedStateHandle,
            testDispatcher,
            testDispatcher,
        )
    }

    private fun createViewModelWithFiles(vararg files: FileItem): FileExplorerViewModel {
        fakeRepository.filesToReturn = files.toList()
        return createViewModel().also { viewModel = it }
    }

    private fun navigateToSubdir(name: String = "subdir") {
        val dir = testDirectoryFileItem(name, path = "$storagePath/$name")
        viewModel.navigateTo(dir)
    }

    @Test
    fun `initial state loads files`() = runTest {
        createViewModelWithFiles(testFileItem("file1.txt"), testFileItem("file2.txt"))

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
        createViewModelWithFiles()

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

    @Test
    fun `refreshFiles updates list without loading state`() = runTest {
        createViewModelWithFiles(testFileItem("old.txt"))

        viewModel.uiState.test {
            assertEquals(listOf("old.txt"), awaitItem().files.map { it.name })

            fakeRepository.filesToReturn = listOf(testFileItem("new.txt"))
            viewModel.refreshFiles()

            val refreshed = awaitItem()
            assertEquals(listOf("new.txt"), refreshed.files.map { it.name })
            assertFalse(refreshed.isLoading)
        }
    }

    @Test
    fun `enterSelectionMode enables selection with file`() = runTest {
        val file = testFileItem("photo.jpg")
        viewModel.enterSelectionMode(file)

        val state = viewModel.uiState.value
        assertTrue(state.isSelectionMode)
        assertTrue(file.path in state.selectedFiles)
        assertEquals(1, state.selectedFiles.size)
    }

    @Test
    fun `toggleFileSelection adds and removes files`() = runTest {
        val file1 = testFileItem("a.txt")
        val file2 = testFileItem("b.txt")
        viewModel.enterSelectionMode(file1)

        viewModel.toggleFileSelection(file2)
        assertEquals(2, viewModel.uiState.value.selectedFiles.size)

        viewModel.toggleFileSelection(file1)
        assertEquals(1, viewModel.uiState.value.selectedFiles.size)
        assertTrue(file2.path in viewModel.uiState.value.selectedFiles)
    }

    @Test
    fun `toggleFileSelection exits selection mode when last file deselected`() = runTest {
        val file = testFileItem("a.txt")
        viewModel.enterSelectionMode(file)

        viewModel.toggleFileSelection(file)

        val state = viewModel.uiState.value
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedFiles.isEmpty())
    }

    @Test
    fun `clearSelection exits selection mode`() = runTest {
        viewModel.enterSelectionMode(testFileItem("a.txt"))
        assertTrue(viewModel.uiState.value.isSelectionMode)

        viewModel.clearSelection()

        val state = viewModel.uiState.value
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedFiles.isEmpty())
    }

    @Test
    fun `deleteSelectedFiles trashes files and clears selection`() = runTest {
        val file1 = testFileItem("a.txt")
        val file2 = testFileItem("b.txt")
        createViewModelWithFiles(file1, file2)

        viewModel.enterSelectionMode(file1)
        viewModel.toggleFileSelection(file2)
        viewModel.deleteSelectedFiles()

        assertEquals(2, fakeTrashManager.trashedFiles.size)
        val state = viewModel.uiState.value
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedFiles.isEmpty())
    }

    @Test
    fun `moveToClipboard stores files and exits selection`() = runTest {
        val file1 = testFileItem("a.txt")
        val file2 = testFileItem("b.txt")
        createViewModelWithFiles(file1, file2)

        viewModel.enterSelectionMode(file1)
        viewModel.toggleFileSelection(file2)
        viewModel.moveToClipboard()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isSelectionMode)
        assertTrue(uiState.selectedFiles.isEmpty())
        assertEquals(ClipboardOperation.MOVE, uiState.clipboardOperation)

        val clipboard = fakeClipboardManager.state.value
        assertEquals(2, clipboard.files.size)
        assertEquals(ClipboardOperation.MOVE, clipboard.operation)
    }

    @Test
    fun `copyToClipboard stores files and exits selection`() = runTest {
        val file1 = testFileItem("a.txt")
        createViewModelWithFiles(file1)

        viewModel.enterSelectionMode(file1)
        viewModel.copyToClipboard()

        assertEquals(ClipboardOperation.COPY, viewModel.uiState.value.clipboardOperation)

        val clipboard = fakeClipboardManager.state.value
        assertEquals(1, clipboard.files.size)
        assertEquals(ClipboardOperation.COPY, clipboard.operation)
    }

    @Test
    fun `cancelClipboard clears clipboard`() = runTest {
        val file1 = testFileItem("a.txt")
        createViewModelWithFiles(file1)

        viewModel.enterSelectionMode(file1)
        viewModel.moveToClipboard()
        assertTrue(fakeClipboardManager.state.value.hasContent)

        viewModel.cancelClipboard()

        assertFalse(fakeClipboardManager.state.value.hasContent)
        assertNull(viewModel.uiState.value.clipboardOperation)
    }

    @Test
    fun `pasteFiles with MOVE calls moveFile and clears clipboard`() = runTest {
        val file1 = testFileItem("a.txt")
        val file2 = testFileItem("b.txt")
        createViewModelWithFiles(file1, file2)

        viewModel.enterSelectionMode(file1)
        viewModel.toggleFileSelection(file2)
        viewModel.moveToClipboard()
        viewModel.pasteFiles()

        assertEquals(2, fakeMoveManager.movedFiles.size)
        assertTrue(fakeMoveManager.copiedFiles.isEmpty())
        assertFalse(fakeClipboardManager.state.value.hasContent)
        assertNull(viewModel.uiState.value.clipboardOperation)
    }

    @Test
    fun `renameSelectedFile renames file and exits selection`() = runTest {
        val file = testFileItem("old.txt")
        createViewModelWithFiles(file)

        viewModel.enterSelectionMode(file)
        viewModel.renameSelectedFile("new.txt")

        assertEquals(1, fakeMoveManager.renamedFiles.size)
        assertEquals(file to "new.txt", fakeMoveManager.renamedFiles.first())
        val state = viewModel.uiState.value
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedFiles.isEmpty())
    }

    @Test
    fun `renameSelectedFile is no-op when no files selected`() = runTest {
        viewModel.renameSelectedFile("new.txt")
        assertTrue(fakeMoveManager.renamedFiles.isEmpty())
    }

    @Test
    fun `createFolder calls fileMoveManager with current path`() = runTest {
        val currentPath = viewModel.uiState.value.currentPath
        viewModel.createFolder("New Folder")

        assertEquals(1, fakeMoveManager.createdFolders.size)
        assertEquals(currentPath to "New Folder", fakeMoveManager.createdFolders.first())
    }

    @Test
    fun `createFolder in subdirectory uses correct parent path`() = runTest {
        navigateToSubdir("Documents")
        val currentPath = viewModel.uiState.value.currentPath

        viewModel.createFolder("Notes")

        assertEquals(1, fakeMoveManager.createdFolders.size)
        assertEquals(currentPath to "Notes", fakeMoveManager.createdFolders.first())
    }

    private fun assertFolderNavigation(sourceType: FileSourceType, expected: Boolean) {
        viewModel = createViewModel(sourceType = sourceType.name)
        assertEquals(expected, viewModel.uiState.value.allowFolderNavigation)
    }

    @Test
    fun `allowFolderNavigation is true for local storage`() = runTest {
        assertFolderNavigation(FileSourceType.LOCAL_STORAGE, expected = true)
    }

    @Test
    fun `allowFolderNavigation is true for downloads`() = runTest {
        assertFolderNavigation(FileSourceType.LOCAL_DOWNLOADS, expected = true)
    }

    @Test
    fun `allowFolderNavigation is false for images`() = runTest {
        assertFolderNavigation(FileSourceType.LOCAL_IMAGES, expected = false)
    }

    @Test
    fun `allowFolderNavigation is false for videos`() = runTest {
        assertFolderNavigation(FileSourceType.LOCAL_VIDEOS, expected = false)
    }

    @Test
    fun `allowFolderNavigation is false for audio`() = runTest {
        assertFolderNavigation(FileSourceType.LOCAL_AUDIO, expected = false)
    }

    @Test
    fun `allowFolderNavigation is false for documents`() = runTest {
        assertFolderNavigation(FileSourceType.LOCAL_DOCUMENTS, expected = false)
    }

    @Test
    fun `allowFolderNavigation is false for apps`() = runTest {
        assertFolderNavigation(FileSourceType.LOCAL_APPS, expected = false)
    }

    @Test
    fun `allowFolderNavigation is false for trash`() = runTest {
        assertFolderNavigation(FileSourceType.LOCAL_TRASH, expected = false)
    }

    @Test
    fun `pasteFiles with COPY calls copyFile and clears clipboard`() = runTest {
        val file1 = testFileItem("a.txt")
        createViewModelWithFiles(file1)

        viewModel.enterSelectionMode(file1)
        viewModel.copyToClipboard()
        viewModel.pasteFiles()

        assertEquals(1, fakeMoveManager.copiedFiles.size)
        assertTrue(fakeMoveManager.movedFiles.isEmpty())
        assertFalse(fakeClipboardManager.state.value.hasContent)
        assertNull(viewModel.uiState.value.clipboardOperation)
    }
}
