package com.jonecx.ibex.ui.explorer

import android.os.Environment
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.RecentFolder
import com.jonecx.ibex.data.model.SortOption
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.data.preferences.RecentFoldersPreferencesContract
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract.Companion.DEFAULT_GRID_COLUMNS
import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.data.repository.FileClipboardManager
import com.jonecx.ibex.data.repository.FileMoveManager
import com.jonecx.ibex.data.repository.FileRepository
import com.jonecx.ibex.data.repository.FileTrashManager
import com.jonecx.ibex.data.repository.MediaType
import com.jonecx.ibex.di.FileRepositoryFactory
import com.jonecx.ibex.di.MainDispatcher
import com.jonecx.ibex.util.launchCollect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@Immutable
data class ScrollPosition(val firstVisibleItemIndex: Int = 0, val firstVisibleItemScrollOffset: Int = 0)

@Immutable
data class FileExplorerUiState(
    val currentPath: String = INTERNAL_STORAGE_PATH,
    val files: List<FileItem> = emptyList(),
    val selectedFile: FileItem? = null,
    val isLoading: Boolean = true,
    val error: Throwable? = null,
    val navigationStack: List<String> = listOf(INTERNAL_STORAGE_PATH),
    val rootPath: String = INTERNAL_STORAGE_PATH,
    val allowFolderNavigation: Boolean = true,
    val isAtInternalStorageRoot: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    val gridColumns: Int = DEFAULT_GRID_COLUMNS,
    val isSelectionMode: Boolean = false,
    val selectedFiles: Set<String> = emptySet(),
    val clipboardOperation: ClipboardOperation? = null,
    val isRemoteBrowsing: Boolean = false,
    val restoredScrollPosition: ScrollPosition? = null,
    val sortOption: SortOption = SortOption.DEFAULT,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
) {
    val canCreateFolder: Boolean get() = allowFolderNavigation
    val displayFiles: List<FileItem> = if (searchQuery.isEmpty()) {
        files
    } else {
        files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
}

val INTERNAL_STORAGE_PATH: String = Environment.getExternalStorageDirectory().absolutePath

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    private val repositoryFactory: FileRepositoryFactory,
    private val settingsPreferences: SettingsPreferencesContract,
    private val recentFoldersPreferences: RecentFoldersPreferencesContract,
    private val fileTrashManager: FileTrashManager,
    private val fileMoveManager: FileMoveManager,
    private val clipboardManager: FileClipboardManager,
    savedStateHandle: SavedStateHandle,
    @MainDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    companion object {
        const val ARG_SOURCE_TYPE = "sourceType"
        const val ARG_ROOT_PATH = "rootPath"
        const val ARG_TITLE = "title"
        const val ARG_CONNECTION_ID = "connectionId"
    }

    private val sourceType: FileSourceType = FileSourceType.valueOf(
        savedStateHandle.get<String>(ARG_SOURCE_TYPE) ?: FileSourceType.LOCAL_STORAGE.name,
    )
    private val initialPath: String? = savedStateHandle.decodedString(ARG_ROOT_PATH)
    private val title: String? = savedStateHandle.decodedString(ARG_TITLE)
    private val connectionId: String? = savedStateHandle.decodedString(ARG_CONNECTION_ID)

    private val repository: FileRepository = createRepository(sourceType)
    private val allowFolderNavigation: Boolean = sourceType in listOf(
        FileSourceType.LOCAL_STORAGE,
        FileSourceType.LOCAL_DOWNLOADS,
        FileSourceType.SMB,
    )
    private val startPath = initialPath ?: title ?: INTERNAL_STORAGE_PATH

    private val isRemote: Boolean = sourceType == FileSourceType.SMB

    private val _uiState = MutableStateFlow(
        FileExplorerUiState(
            currentPath = startPath,
            navigationStack = listOf(startPath),
            rootPath = startPath,
            allowFolderNavigation = allowFolderNavigation,
            isRemoteBrowsing = isRemote,
        ),
    )
    val uiState: StateFlow<FileExplorerUiState> = _uiState.asStateFlow()

    private val _recentFolders = MutableStateFlow<List<RecentFolder>>(emptyList())
    val recentFolders: StateFlow<List<RecentFolder>> = _recentFolders.asStateFlow()

    private var loadFilesJob: Job? = null
    private val scrollPositions = mutableMapOf<String, ScrollPosition>()

    private fun List<FileItem>.applySorting(
        option: SortOption = _uiState.value.sortOption,
    ): List<FileItem> = sortedWith(option.toComparator())

    init {
        if (!allowFolderNavigation && title != null) {
            _uiState.value = _uiState.value.copy(currentPath = title)
        }
        loadFiles(startPath)
        viewModelScope.launchCollect(settingsPreferences.viewMode, dispatcher) { mode ->
            _uiState.update { it.copy(viewMode = mode) }
        }
        viewModelScope.launchCollect(settingsPreferences.gridColumns, dispatcher) { columns ->
            _uiState.update { it.copy(gridColumns = columns) }
        }
        viewModelScope.launchCollect(clipboardManager.state, dispatcher) { clipboard ->
            _uiState.update { it.copy(clipboardOperation = clipboard.operation) }
        }
        viewModelScope.launchCollect(recentFoldersPreferences.recentFolders, dispatcher) { folders ->
            _recentFolders.value = folders
        }
        viewModelScope.launchCollect(settingsPreferences.sortOption, dispatcher) { option ->
            _uiState.update { state ->
                state.copy(
                    sortOption = option,
                    files = state.files.applySorting(option),
                )
            }
        }
    }

    private fun createRepository(sourceType: FileSourceType): FileRepository {
        return when (sourceType) {
            FileSourceType.LOCAL_STORAGE,
            FileSourceType.LOCAL_DOWNLOADS,
            -> repositoryFactory.createLocalFileRepository()
            FileSourceType.LOCAL_IMAGES -> repositoryFactory.createMediaFileRepository(MediaType.IMAGES)
            FileSourceType.LOCAL_VIDEOS -> repositoryFactory.createMediaFileRepository(MediaType.VIDEOS)
            FileSourceType.LOCAL_AUDIO -> repositoryFactory.createMediaFileRepository(MediaType.AUDIO)
            FileSourceType.LOCAL_DOCUMENTS -> repositoryFactory.createMediaFileRepository(MediaType.DOCUMENTS)
            FileSourceType.LOCAL_APPS -> repositoryFactory.createAppsRepository()
            FileSourceType.LOCAL_RECENT -> repositoryFactory.createRecentFilesRepository()
            FileSourceType.LOCAL_TRASH -> repositoryFactory.createTrashRepository()
            FileSourceType.SMB -> repositoryFactory.createSmbFileRepository(
                connectionId ?: error("connectionId required for SMB"),
            )
            else -> repositoryFactory.createLocalFileRepository()
        }
    }

    fun loadFiles(path: String, showLoading: Boolean = true) {
        loadFilesJob?.cancel()
        loadFilesJob = viewModelScope.launch(dispatcher) {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            repository.getFiles(path)
                .catch { e ->
                    if (showLoading) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e,
                            )
                        }
                    }
                }
                .collect { files ->
                    val isAtRoot = path == INTERNAL_STORAGE_PATH
                    _uiState.update {
                        it.copy(
                            currentPath = path,
                            files = files.applySorting(it.sortOption),
                            isLoading = false,
                            error = null,
                            isAtInternalStorageRoot = isAtRoot,
                        )
                    }
                }
        }
    }

    fun saveScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        val path = _uiState.value.currentPath
        scrollPositions[path] = ScrollPosition(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    }

    fun navigateTo(fileItem: FileItem) {
        if (fileItem.isDirectory && allowFolderNavigation) {
            val newStack = _uiState.value.navigationStack + fileItem.path
            _uiState.update {
                it.copy(
                    navigationStack = newStack,
                    selectedFile = null,
                    restoredScrollPosition = null,
                ).dismissSearch()
            }
            trackRecentFolder(fileItem.path, fileItem.name)
            loadFiles(fileItem.path)
        } else {
            _uiState.update { it.copy(selectedFile = fileItem) }
        }
    }

    fun navigateUp(): Boolean {
        val stack = _uiState.value.navigationStack
        if (stack.size > 1) {
            val newStack = stack.dropLast(1)
            val parentPath = newStack.last()
            val restored = scrollPositions.remove(parentPath)
            _uiState.update {
                it.copy(
                    navigationStack = newStack,
                    selectedFile = null,
                    restoredScrollPosition = restored,
                ).dismissSearch()
            }
            loadFiles(parentPath)
            return true
        }
        return false
    }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch(dispatcher) {
            settingsPreferences.setSortOption(option)
        }
    }

    fun activateSearch() {
        _uiState.update { it.copy(isSearchActive = true, searchQuery = "") }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearSearch() {
        _uiState.update { it.dismissSearch() }
    }

    fun refreshFiles() {
        loadFiles(_uiState.value.currentPath, showLoading = false)
    }

    fun selectFile(fileItem: FileItem?) {
        _uiState.update { it.copy(selectedFile = fileItem) }
    }

    fun getCurrentDirectoryName(): String? {
        if (!allowFolderNavigation) {
            return _uiState.value.currentPath
        }
        val path = _uiState.value.currentPath
        return when {
            path == INTERNAL_STORAGE_PATH -> null
            else -> path.trimEnd('/').substringAfterLast('/')
        }
    }

    fun setTitle(title: String) {
        _uiState.update { it.copy(currentPath = title) }
    }

    fun canNavigateUp(): Boolean {
        return _uiState.value.navigationStack.size > 1
    }

    fun enterSelectionMode(fileItem: FileItem) {
        _uiState.update {
            it.copy(
                isSelectionMode = true,
                selectedFiles = setOf(fileItem.path),
            )
        }
    }

    fun toggleFileSelection(fileItem: FileItem) {
        _uiState.update { state ->
            val newSelection = if (fileItem.path in state.selectedFiles) {
                state.selectedFiles - fileItem.path
            } else {
                state.selectedFiles + fileItem.path
            }
            if (newSelection.isEmpty()) {
                state.copy(isSelectionMode = false, selectedFiles = emptySet())
            } else {
                state.copy(selectedFiles = newSelection)
            }
        }
    }

    fun clearSelection() {
        _uiState.update { it.exitSelectionMode() }
    }

    fun deleteSelectedFiles() {
        val filesToDelete = _uiState.value.selectedFileItems()
        if (filesToDelete.isEmpty()) return

        viewModelScope.launch(dispatcher) {
            filesToDelete.map { file ->
                async {
                    if (isRemote) {
                        fileMoveManager.deleteFile(file)
                    } else {
                        fileTrashManager.trashFile(file)
                    }
                }
            }.awaitAll()
            _uiState.update { it.exitSelectionMode() }
            refreshFiles()
        }
    }

    fun moveToClipboard() = setClipboardFromSelection(ClipboardOperation.MOVE)

    fun copyToClipboard() = setClipboardFromSelection(ClipboardOperation.COPY)

    private fun setClipboardFromSelection(operation: ClipboardOperation) {
        val files = _uiState.value.selectedFileItems()
        if (files.isEmpty()) return
        clipboardManager.setClipboard(files, operation)
        _uiState.update { it.exitSelectionMode() }
    }

    fun renameSelectedFile(newName: String) {
        val file = _uiState.value.selectedFileItems().firstOrNull() ?: return

        viewModelScope.launch(dispatcher) {
            fileMoveManager.renameFile(file, newName)
            _uiState.update { it.exitSelectionMode() }
            refreshFiles()
        }
    }

    fun createFolder(name: String) {
        val parentDir = _uiState.value.currentPath

        viewModelScope.launch(dispatcher) {
            fileMoveManager.createFolder(parentDir, name)
            refreshFiles()
        }
    }

    fun cancelClipboard() {
        clipboardManager.clear()
    }

    fun pasteFiles() {
        val destDir = _uiState.value.currentPath

        viewModelScope.launch(dispatcher) {
            clipboardManager.paste(destDir)
            refreshFiles()
        }
    }

    fun navigateToPath(path: String) {
        val displayName = path.trimEnd('/').substringAfterLast('/')
        _uiState.update {
            it.copy(
                navigationStack = listOf(it.rootPath, path),
                selectedFile = null,
                restoredScrollPosition = null,
            ).dismissSearch()
        }
        trackRecentFolder(path, displayName)
        loadFiles(path)
    }

    fun clearRecentFolders() {
        viewModelScope.launch(dispatcher) {
            recentFoldersPreferences.clearRecentFolders()
        }
    }

    private fun trackRecentFolder(path: String, displayName: String) {
        viewModelScope.launch(dispatcher) {
            recentFoldersPreferences.addRecentFolder(
                RecentFolder(
                    path = path,
                    displayName = displayName,
                    timestamp = System.currentTimeMillis(),
                    sourceType = sourceType.name,
                    connectionId = connectionId,
                ),
            )
        }
    }
}

private fun FileExplorerUiState.dismissSearch() = copy(
    isSearchActive = false,
    searchQuery = "",
)

private fun FileExplorerUiState.exitSelectionMode() = copy(
    isSelectionMode = false,
    selectedFiles = emptySet(),
)

private fun FileExplorerUiState.selectedFileItems(): List<FileItem> =
    files.filter { it.path in selectedFiles }

private fun SavedStateHandle.decodedString(key: String): String? =
    get<String>(key)?.let { if (it.isNotEmpty()) URLDecoder.decode(it, "UTF-8") else null }
