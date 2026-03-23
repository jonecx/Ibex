package com.jonecx.ibex.ui.explorer

import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.SortDirection
import com.jonecx.ibex.data.model.SortField
import com.jonecx.ibex.data.model.SortOption
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.fixtures.FakeFileClipboardManager
import com.jonecx.ibex.fixtures.FakeFileMoveManager
import com.jonecx.ibex.fixtures.FakeFileRepository
import com.jonecx.ibex.fixtures.FakeFileRepositoryFactory
import com.jonecx.ibex.fixtures.FakeFileTrashManager
import com.jonecx.ibex.fixtures.FakeRecentFoldersPreferences
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import com.jonecx.ibex.fixtures.testDirectoryFileItem
import com.jonecx.ibex.fixtures.testFileItem
import com.jonecx.ibex.fixtures.testRemoteDirectoryFileItem
import com.jonecx.ibex.fixtures.testRemoteFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    private lateinit var fakeRecentFolders: FakeRecentFoldersPreferences
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
        fakeRecentFolders = FakeRecentFoldersPreferences()
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
        connectionId: String = "",
    ): FileExplorerViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                FileExplorerViewModel.ARG_SOURCE_TYPE to sourceType,
                FileExplorerViewModel.ARG_ROOT_PATH to rootPath,
                FileExplorerViewModel.ARG_TITLE to title,
                FileExplorerViewModel.ARG_CONNECTION_ID to connectionId,
            ),
        )
        return FileExplorerViewModel(
            fakeFactory,
            fakePreferences,
            fakeRecentFolders,
            fakeTrashManager,
            fakeMoveManager,
            fakeClipboardManager,
            savedStateHandle,
            testDispatcher,
        )
    }

    private fun createViewModelWithFiles(vararg files: FileItem): FileExplorerViewModel {
        fakeRepository.filesToReturn = files.toList()
        return createViewModel().also { viewModel = it }
    }

    private fun createSmbViewModel(): FileExplorerViewModel {
        return createViewModel(
            sourceType = FileSourceType.SMB.name,
            connectionId = TEST_SMB_CONNECTION_ID,
        ).also { viewModel = it }
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

    private fun assertFolderNavigation(
        sourceType: FileSourceType,
        expected: Boolean,
        connectionId: String = "",
    ) {
        viewModel = createViewModel(sourceType = sourceType.name, connectionId = connectionId)
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

    // SMB / Remote browsing tests

    @Test
    fun `SMB source sets isRemoteBrowsing true`() = runTest {
        createSmbViewModel()
        assertTrue(viewModel.uiState.value.isRemoteBrowsing)
    }

    @Test
    fun `SMB source allows folder navigation`() = runTest {
        createSmbViewModel()
        assertTrue(viewModel.uiState.value.allowFolderNavigation)
    }

    @Test
    fun `SMB source allows canCreateFolder`() = runTest {
        createSmbViewModel()
        assertTrue(viewModel.uiState.value.canCreateFolder)
    }

    @Test
    fun `local source sets isRemoteBrowsing false`() = runTest {
        assertFalse(viewModel.uiState.value.isRemoteBrowsing)
    }

    @Test
    fun `local source allows canCreateFolder`() = runTest {
        assertTrue(viewModel.uiState.value.canCreateFolder)
    }

    @Test
    fun `SMB source navigates directories`() = runTest {
        fakeRepository.filesToReturn = listOf(
            testRemoteDirectoryFileItem("share1"),
        )
        createSmbViewModel()

        val dir = testRemoteDirectoryFileItem("subfolder")
        viewModel.navigateTo(dir)

        val state = viewModel.uiState.value
        assertEquals(2, state.navigationStack.size)
        assertTrue(state.navigationStack.last().contains("subfolder"))
    }

    @Test
    fun `SMB source navigateUp works`() = runTest {
        createSmbViewModel()

        val dir = testRemoteDirectoryFileItem("subfolder")
        viewModel.navigateTo(dir)
        assertTrue(viewModel.canNavigateUp())

        viewModel.navigateUp()
        assertFalse(viewModel.canNavigateUp())
    }

    @Test
    fun `allowFolderNavigation is true for SMB`() = runTest {
        assertFolderNavigation(FileSourceType.SMB, expected = true, connectionId = TEST_SMB_CONNECTION_ID)
    }

    // Scroll position save/restore tests

    @Test
    fun `saveScrollPosition stores position for current path`() = runTest {
        createViewModelWithFiles(testFileItem("file1.txt"))

        viewModel.saveScrollPosition(10, 50)

        val state = viewModel.uiState.value
        assertNull(state.restoredScrollPosition)
    }

    @Test
    fun `navigateUp restores saved scroll position`() = runTest {
        createViewModelWithFiles(testDirectoryFileItem("subdir"))

        viewModel.saveScrollPosition(10, 50)
        navigateToSubdir()

        viewModel.navigateUp()

        val restored = viewModel.uiState.value.restoredScrollPosition
        assertNotNull(restored)
        assertEquals(10, restored!!.firstVisibleItemIndex)
        assertEquals(50, restored.firstVisibleItemScrollOffset)
    }

    @Test
    fun `navigateUp without saved position has null restoredScrollPosition`() = runTest {
        createViewModelWithFiles(testDirectoryFileItem("subdir"))

        navigateToSubdir()
        viewModel.navigateUp()

        assertNull(viewModel.uiState.value.restoredScrollPosition)
    }

    @Test
    fun `navigateTo clears restoredScrollPosition`() = runTest {
        createViewModelWithFiles(testDirectoryFileItem("subdir"))

        viewModel.saveScrollPosition(5, 20)
        navigateToSubdir()

        assertNull(viewModel.uiState.value.restoredScrollPosition)
    }

    @Test
    fun `scroll position restored only once per navigateUp`() = runTest {
        createViewModelWithFiles(testDirectoryFileItem("subdir"))

        viewModel.saveScrollPosition(10, 50)
        navigateToSubdir()

        viewModel.navigateUp()
        assertNotNull(viewModel.uiState.value.restoredScrollPosition)

        navigateToSubdir()
        viewModel.navigateUp()
        assertNull(viewModel.uiState.value.restoredScrollPosition)
    }

    @Test
    fun `nested navigation restores correct scroll positions`() = runTest {
        val subdir1 = testDirectoryFileItem("level1", path = "$storagePath/level1")
        val subdir2 = testDirectoryFileItem("level2", path = "$storagePath/level1/level2")
        fakeRepository.filesToReturn = listOf(subdir1)
        viewModel = createViewModel()

        viewModel.saveScrollPosition(5, 10)
        viewModel.navigateTo(subdir1)

        viewModel.saveScrollPosition(15, 30)
        viewModel.navigateTo(subdir2)

        viewModel.navigateUp()
        val restoredLevel1 = viewModel.uiState.value.restoredScrollPosition
        assertNotNull(restoredLevel1)
        assertEquals(15, restoredLevel1!!.firstVisibleItemIndex)
        assertEquals(30, restoredLevel1.firstVisibleItemScrollOffset)

        viewModel.navigateUp()
        val restoredRoot = viewModel.uiState.value.restoredScrollPosition
        assertNotNull(restoredRoot)
        assertEquals(5, restoredRoot!!.firstVisibleItemIndex)
        assertEquals(10, restoredRoot.firstVisibleItemScrollOffset)
    }

    // Sort tests

    @Test
    fun `default sort is NAME ASCENDING`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(SortOption.DEFAULT, state.sortOption)
        assertEquals(SortField.NAME, state.sortOption.field)
        assertEquals(SortDirection.ASCENDING, state.sortOption.direction)
    }

    @Test
    fun `files sorted by name ascending by default`() = runTest {
        val b = testFileItem("banana.txt", size = 300)
        val a = testFileItem("apple.txt", size = 100)
        val c = testFileItem("cherry.txt", size = 200)
        createViewModelWithFiles(b, a, c)

        val names = viewModel.uiState.value.files.map { it.name }
        assertEquals(listOf("apple.txt", "banana.txt", "cherry.txt"), names)
    }

    @Test
    fun `directories always sort before files`() = runTest {
        val file = testFileItem("aaa.txt", size = 100)
        val dir = testDirectoryFileItem("zzz_folder")
        createViewModelWithFiles(file, dir)

        val names = viewModel.uiState.value.files.map { it.name }
        assertEquals(listOf("zzz_folder", "aaa.txt"), names)
    }

    @Test
    fun `setSortOption changes sort and re-sorts files`() = runTest {
        val small = testFileItem("big_name.txt", size = 10)
        val large = testFileItem("aaa.txt", size = 9999)
        createViewModelWithFiles(small, large)

        assertEquals(listOf("aaa.txt", "big_name.txt"), viewModel.uiState.value.files.map { it.name })

        viewModel.setSortOption(SortOption(SortField.SIZE, SortDirection.DESCENDING))

        val sorted = viewModel.uiState.value.files.map { it.name }
        assertEquals(listOf("aaa.txt", "big_name.txt"), sorted)
        assertEquals(9999L, viewModel.uiState.value.files.first().size)
    }

    @Test
    fun `sort by size ascending`() = runTest {
        val big = testFileItem("big.txt", size = 9000)
        val small = testFileItem("small.txt", size = 100)
        val mid = testFileItem("mid.txt", size = 3000)
        fakePreferences.setSortOption(SortOption(SortField.SIZE, SortDirection.ASCENDING))
        createViewModelWithFiles(big, small, mid)

        val names = viewModel.uiState.value.files.map { it.name }
        assertEquals(listOf("small.txt", "mid.txt", "big.txt"), names)
    }

    @Test
    fun `sort by name descending`() = runTest {
        val a = testFileItem("apple.txt")
        val b = testFileItem("banana.txt")
        val c = testFileItem("cherry.txt")
        fakePreferences.setSortOption(SortOption(SortField.NAME, SortDirection.DESCENDING))
        createViewModelWithFiles(a, b, c)

        val names = viewModel.uiState.value.files.map { it.name }
        assertEquals(listOf("cherry.txt", "banana.txt", "apple.txt"), names)
    }

    @Test
    fun `sort by date modified descending`() = runTest {
        val old = testFileItem("old.txt").copy(lastModified = 1000L)
        val recent = testFileItem("recent.txt").copy(lastModified = 9000L)
        val mid = testFileItem("mid.txt").copy(lastModified = 5000L)
        fakePreferences.setSortOption(SortOption(SortField.DATE_MODIFIED, SortDirection.DESCENDING))
        fakeRepository.filesToReturn = listOf(old, recent, mid)
        viewModel = createViewModel()

        val names = viewModel.uiState.value.files.map { it.name }
        assertEquals(listOf("recent.txt", "mid.txt", "old.txt"), names)
    }

    @Test
    fun `sort preference change persists via preferences`() = runTest {
        val option = SortOption(SortField.SIZE, SortDirection.DESCENDING)
        viewModel.setSortOption(option)

        assertEquals(option, fakePreferences.currentSortOption())
    }

    @Test
    fun `sort by date created ascending`() = runTest {
        val newer = testFileItem("newer.txt").copy(createdAt = 8000L)
        val older = testFileItem("older.txt").copy(createdAt = 2000L)
        val mid = testFileItem("mid.txt").copy(createdAt = 5000L)
        fakePreferences.setSortOption(SortOption(SortField.DATE_CREATED, SortDirection.ASCENDING))
        fakeRepository.filesToReturn = listOf(newer, older, mid)
        viewModel = createViewModel()

        val names = viewModel.uiState.value.files.map { it.name }
        assertEquals(listOf("older.txt", "mid.txt", "newer.txt"), names)
    }

    @Test
    fun `sort by date created descending`() = runTest {
        val newer = testFileItem("newer.txt").copy(createdAt = 8000L)
        val older = testFileItem("older.txt").copy(createdAt = 2000L)
        val mid = testFileItem("mid.txt").copy(createdAt = 5000L)
        fakePreferences.setSortOption(SortOption(SortField.DATE_CREATED, SortDirection.DESCENDING))
        fakeRepository.filesToReturn = listOf(newer, older, mid)
        viewModel = createViewModel()

        val names = viewModel.uiState.value.files.map { it.name }
        assertEquals(listOf("newer.txt", "mid.txt", "older.txt"), names)
    }

    @Test
    fun `sort preserves directories first regardless of sort field`() = runTest {
        val bigFile = testFileItem("big.txt", size = 99999)
        val smallDir = testDirectoryFileItem("tiny_dir")
        fakePreferences.setSortOption(SortOption(SortField.SIZE, SortDirection.DESCENDING))
        createViewModelWithFiles(bigFile, smallDir)

        val names = viewModel.uiState.value.files.map { it.name }
        assertEquals("tiny_dir", names.first())
    }

    // Search tests

    @Test
    fun `activateSearch sets isSearchActive`() = runTest {
        viewModel.activateSearch()
        assertTrue(viewModel.uiState.value.isSearchActive)
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `setSearchQuery filters displayFiles by name`() = runTest {
        val apple = testFileItem("apple.txt")
        val banana = testFileItem("banana.txt")
        val apricot = testFileItem("apricot.txt")
        createViewModelWithFiles(apple, banana, apricot)

        viewModel.activateSearch()
        viewModel.setSearchQuery("ap")

        val display = viewModel.uiState.value.displayFiles.map { it.name }
        assertEquals(listOf("apple.txt", "apricot.txt"), display)
    }

    @Test
    fun `search is case insensitive`() = runTest {
        val file = testFileItem("README.md")
        createViewModelWithFiles(file)

        viewModel.activateSearch()
        viewModel.setSearchQuery("readme")

        assertEquals(1, viewModel.uiState.value.displayFiles.size)
    }

    @Test
    fun `clearSearch resets isSearchActive and query`() = runTest {
        val file = testFileItem("file.txt")
        createViewModelWithFiles(file)

        viewModel.activateSearch()
        viewModel.setSearchQuery("xyz")
        assertTrue(viewModel.uiState.value.isSearchActive)

        viewModel.clearSearch()
        assertFalse(viewModel.uiState.value.isSearchActive)
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `displayFiles returns all files when search query is empty`() = runTest {
        val a = testFileItem("a.txt")
        val b = testFileItem("b.txt")
        createViewModelWithFiles(a, b)

        viewModel.activateSearch()
        assertEquals(2, viewModel.uiState.value.displayFiles.size)
    }

    @Test
    fun `navigateTo clears search`() = runTest {
        val dir = testDirectoryFileItem("subdir")
        createViewModelWithFiles(dir)

        viewModel.activateSearch()
        viewModel.setSearchQuery("sub")
        viewModel.navigateTo(dir)

        assertFalse(viewModel.uiState.value.isSearchActive)
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `navigateUp clears search`() = runTest {
        val dir = testDirectoryFileItem("subdir")
        createViewModelWithFiles(dir)
        viewModel.navigateTo(dir)

        viewModel.activateSearch()
        viewModel.setSearchQuery("test")
        viewModel.navigateUp()

        assertFalse(viewModel.uiState.value.isSearchActive)
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    companion object {
        private const val TEST_SMB_CONNECTION_ID = "smb-1"
    }

    @Test
    fun `deleteSelectedFiles uses trashManager for local files`() = runTest {
        val file = testFileItem("local.txt")
        createViewModelWithFiles(file)

        viewModel.enterSelectionMode(file)
        viewModel.deleteSelectedFiles()

        assertEquals(1, fakeTrashManager.trashedFiles.size)
        assertTrue(fakeMoveManager.deletedFiles.isEmpty())
        assertFalse(viewModel.uiState.value.isSelectionMode)
    }

    @Test
    fun `deleteSelectedFiles uses fileMoveManager for remote files`() = runTest {
        val file = testRemoteFileItem("remote.txt")
        fakeRepository.filesToReturn = listOf(file)
        viewModel = createSmbViewModel()

        viewModel.enterSelectionMode(file)
        viewModel.deleteSelectedFiles()

        assertEquals(1, fakeMoveManager.deletedFiles.size)
        assertTrue(fakeTrashManager.trashedFiles.isEmpty())
        assertFalse(viewModel.uiState.value.isSelectionMode)
    }

    @Test
    fun `SMB createFolder calls fileMoveManager with remote path`() = runTest {
        fakeRepository.filesToReturn = listOf(testRemoteFileItem("file.txt"))
        createSmbViewModel()
        val currentPath = viewModel.uiState.value.currentPath

        viewModel.createFolder("Remote Folder")

        assertEquals(1, fakeMoveManager.createdFolders.size)
        assertEquals(currentPath to "Remote Folder", fakeMoveManager.createdFolders.first())
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

    // Recent folders tests

    @Test
    fun `navigateTo directory tracks recent folder`() = runTest {
        val dir = testDirectoryFileItem("Documents", path = "$storagePath/Documents")
        viewModel.navigateTo(dir)

        val recents = fakeRecentFolders.currentRecents()
        assertEquals(1, recents.size)
        assertEquals("$storagePath/Documents", recents.first().path)
        assertEquals("Documents", recents.first().displayName)
        assertEquals(FileSourceType.LOCAL_STORAGE.name, recents.first().sourceType)
    }

    @Test
    fun `navigateTo file does not track recent folder`() = runTest {
        val file = testFileItem("photo.jpg")
        viewModel.navigateTo(file)

        assertTrue(fakeRecentFolders.currentRecents().isEmpty())
    }

    @Test
    fun `navigateToPath updates state and tracks recent`() = runTest {
        val path = "$storagePath/Pictures/Vacation"
        viewModel.navigateToPath(path)

        val state = viewModel.uiState.value
        assertEquals(path, state.navigationStack.last())
        assertEquals(2, state.navigationStack.size)

        val recents = fakeRecentFolders.currentRecents()
        assertEquals(1, recents.size)
        assertEquals(path, recents.first().path)
        assertEquals("Vacation", recents.first().displayName)
    }

    @Test
    fun `navigateToPath clears search`() = runTest {
        viewModel.activateSearch()
        viewModel.setSearchQuery("test")

        viewModel.navigateToPath("$storagePath/Downloads")

        assertFalse(viewModel.uiState.value.isSearchActive)
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `clearRecentFolders clears preferences`() = runTest {
        val dir = testDirectoryFileItem("Music", path = "$storagePath/Music")
        viewModel.navigateTo(dir)
        assertFalse(fakeRecentFolders.currentRecents().isEmpty())

        viewModel.clearRecentFolders()

        assertTrue(fakeRecentFolders.currentRecents().isEmpty())
    }

    @Test
    fun `recentFolders flow reflects preferences`() = runTest {
        viewModel.recentFolders.test {
            assertEquals(emptyList<Any>(), awaitItem())

            val dir = testDirectoryFileItem("DCIM", path = "$storagePath/DCIM")
            viewModel.navigateTo(dir)

            val recents = awaitItem()
            assertEquals(1, recents.size)
            assertEquals("DCIM", recents.first().displayName)
        }
    }

    @Test
    fun `SMB navigateTo tracks recent with connectionId`() = runTest {
        fakeRepository.filesToReturn = listOf(testRemoteDirectoryFileItem("share1"))
        createSmbViewModel()

        val dir = testRemoteDirectoryFileItem("projects")
        viewModel.navigateTo(dir)

        val recents = fakeRecentFolders.currentRecents()
        assertEquals(1, recents.size)
        assertEquals(FileSourceType.SMB.name, recents.first().sourceType)
        assertEquals(TEST_SMB_CONNECTION_ID, recents.first().connectionId)
    }
}
