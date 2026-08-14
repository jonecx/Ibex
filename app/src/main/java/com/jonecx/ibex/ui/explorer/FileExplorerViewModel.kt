package com.jonecx.ibex.ui.explorer

import android.os.Environment
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.NetworkProtocol
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
import com.jonecx.ibex.util.launchCollect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder

@Immutable
data class ScrollPosition(val firstVisibleItemIndex: Int = 0, val firstVisibleItemScrollOffset: Int = 0)

// One tappable segment of the path bar; the root renders as a home icon, the current folder is not clickable.
@Immutable
data class Breadcrumb(
    val index: Int,
    val name: String,
    val isRoot: Boolean,
    val isCurrent: Boolean,
)

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
    val isMediaFolderBrowsing: Boolean = false,
    val restoredScrollPosition: ScrollPosition? = null,
    val sortOption: SortOption = SortOption.DEFAULT,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
) {
    // Media-folder browsing (Images/Videos) reuses folder navigation but is a gallery, not a place to create folders.
    val canCreateFolder: Boolean get() = allowFolderNavigation && !isMediaFolderBrowsing
    val displayFiles: List<FileItem> = if (searchQuery.isEmpty()) {
        files
    } else {
        files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // The path bar mirrors the navigation stack: root as home, each visited folder after it, current last.
    val breadcrumbs: List<Breadcrumb> = if (allowFolderNavigation) {
        navigationStack.mapIndexed { index, path ->
            Breadcrumb(
                index = index,
                name = path.trimEnd('/').substringAfterLast('/'),
                isRoot = index == 0,
                isCurrent = index == navigationStack.lastIndex,
            )
        }
    } else {
        emptyList()
    }
}

val INTERNAL_STORAGE_PATH: String = Environment.getExternalStorageDirectory().absolutePath

class FileExplorerViewModel(
    private val repositoryFactory: FileRepositoryFactory,
    private val settingsPreferences: SettingsPreferencesContract,
    private val recentFoldersPreferences: RecentFoldersPreferencesContract,
    private val fileTrashManager: FileTrashManager,
    private val fileMoveManager: FileMoveManager,
    private val clipboardManager: FileClipboardManager,
    private val analyticsManager: AnalyticsManager,
    savedStateHandle: SavedStateHandle,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    companion object {
        const val ARG_SOURCE_TYPE = "sourceType"
        const val ARG_ROOT_PATH = "rootPath"
        const val ARG_TITLE = "title"
        const val ARG_CONNECTION_ID = "connectionId"
        private const val SEARCH_DEBOUNCE_MS = 500L
    }

    private val sourceType: FileSourceType = FileSourceType.valueOf(
        savedStateHandle.get<String>(ARG_SOURCE_TYPE) ?: FileSourceType.LOCAL_STORAGE.name,
    )
    private val initialPath: String? = savedStateHandle.decodedString(ARG_ROOT_PATH)
    private val title: String? = savedStateHandle.decodedString(ARG_TITLE)
    private val connectionId: String? = savedStateHandle.decodedString(ARG_CONNECTION_ID)

    private val repository: FileRepository = createRepository(sourceType)

    // Images/Videos browse the folder tree filtered to their media, so they group into albums yet still nest.
    private val isMediaFolderBrowsing: Boolean = sourceType in listOf(
        FileSourceType.LOCAL_IMAGES,
        FileSourceType.LOCAL_VIDEOS,
    )
    private val allowFolderNavigation: Boolean = isMediaFolderBrowsing || sourceType in listOf(
        FileSourceType.LOCAL_STORAGE,
        FileSourceType.LOCAL_DOWNLOADS,
        FileSourceType.SMB,
    )

    // Media-folder browsing starts at the storage root; the title is kept only for the top-bar label there.
    private val startPath = if (isMediaFolderBrowsing) INTERNAL_STORAGE_PATH else initialPath ?: title ?: INTERNAL_STORAGE_PATH

    private val isRemote: Boolean = sourceType == FileSourceType.SMB

    private val _uiState = MutableStateFlow(
        FileExplorerUiState(
            currentPath = startPath,
            navigationStack = listOf(startPath),
            rootPath = startPath,
            allowFolderNavigation = allowFolderNavigation,
            isRemoteBrowsing = isRemote,
            isMediaFolderBrowsing = isMediaFolderBrowsing,
        ),
    )
    val uiState: StateFlow<FileExplorerUiState> = _uiState.asStateFlow()

    private val _recentFolders = MutableStateFlow<List<RecentFolder>>(emptyList())
    val recentFolders: StateFlow<List<RecentFolder>> = _recentFolders.asStateFlow()

    private var loadFilesJob: Job? = null
    private var searchJob: Job? = null

    // Guards connection_connect so it fires once per remote-source session, on the first listing.
    private var remoteConnectTracked: Boolean = false
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
            FileSourceType.LOCAL_IMAGES -> repositoryFactory.createMediaFolderRepository(MediaType.IMAGES)
            FileSourceType.LOCAL_VIDEOS -> repositoryFactory.createMediaFolderRepository(MediaType.VIDEOS)
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
            val startMs = System.currentTimeMillis()
            val firstRemoteAttempt = isRemote && !remoteConnectTracked
            if (firstRemoteAttempt) remoteConnectTracked = true
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            repository.getFiles(path)
                .catch { e ->
                    trackLoadOutcome(firstRemoteAttempt, startMs, itemCount = 0, error = e)
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
                    trackLoadOutcome(firstRemoteAttempt, startMs, itemCount = files.size, error = null)
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

    // Fans a directory-listing result out to QoE latency, first-connect, and empty/error signals.
    private fun trackLoadOutcome(firstRemoteAttempt: Boolean, startMs: Long, itemCount: Int, error: Throwable?) {
        val durationMs = System.currentTimeMillis() - startMs
        val success = error == null
        val errorCode = error?.javaClass?.simpleName
        analyticsManager.trackContentLoad(sourceType, isRemote, itemCount, durationMs, success, errorCode)
        if (firstRemoteAttempt) {
            analyticsManager.trackConnectionConnect(NetworkProtocol.SMB, success, durationMs, errorCode)
        }
        when {
            errorCode != null -> analyticsManager.trackContentError(sourceType, isRemote, errorCode)
            itemCount == 0 -> analyticsManager.trackContentEmpty(sourceType, isRemote, context = "folder")
        }
    }

    fun saveScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        val path = _uiState.value.currentPath
        scrollPositions[path] = ScrollPosition(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    }

    fun navigateTo(fileItem: FileItem) {
        if (fileItem.isDirectory && allowFolderNavigation) {
            analyticsManager.trackFileOpen(sourceType, isRemote, fileItem.fileType, isDirectory = true, sizeBytes = fileItem.size)
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
        if (stack.size <= 1) return false
        navigateToStack(stack.dropLast(1))
        return true
    }

    // Loads the last entry of [newStack], restoring its saved scroll and dropping any active search.
    private fun navigateToStack(newStack: List<String>) {
        val targetPath = newStack.last()
        val restored = scrollPositions.remove(targetPath)
        _uiState.update {
            it.copy(
                navigationStack = newStack,
                selectedFile = null,
                restoredScrollPosition = restored,
            ).dismissSearch()
        }
        loadFiles(targetPath)
    }

    // Jumps to an ancestor by truncating the stack to it; the current (last) crumb is a no-op.
    fun navigateToBreadcrumb(index: Int) {
        val stack = _uiState.value.navigationStack
        if (index < 0 || index >= stack.lastIndex) return
        navigateToStack(stack.subList(0, index + 1).toList())
    }

    fun setSortOption(option: SortOption) {
        analyticsManager.trackSortChange(option.field, option.direction)
        viewModelScope.launch(dispatcher) {
            settingsPreferences.setSortOption(option)
        }
    }

    fun activateSearch() {
        analyticsManager.trackSearchStart(sourceType)
        _uiState.update { it.copy(isSearchActive = true, searchQuery = "") }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // Debounce so one search_perform lands per pause in typing, not per keystroke; never the text.
        searchJob?.cancel()
        if (query.isEmpty()) return
        searchJob = viewModelScope.launch(dispatcher) {
            delay(SEARCH_DEBOUNCE_MS)
            analyticsManager.trackSearchPerform(sourceType, query.length, _uiState.value.displayFiles.size)
        }
    }

    fun clearSearch() {
        analyticsManager.trackSearchClear(sourceType)
        searchJob?.cancel()
        _uiState.update { it.dismissSearch() }
    }

    fun refreshFiles() {
        loadFiles(_uiState.value.currentPath, showLoading = false)
    }

    fun selectFile(fileItem: FileItem?) {
        // Opening a non-viewable file into the detail pane; viewable files go through the media viewer.
        if (fileItem != null) {
            analyticsManager.trackFileOpen(sourceType, isRemote, fileItem.fileType, isDirectory = false, sizeBytes = fileItem.size)
        }
        _uiState.update { it.copy(selectedFile = fileItem) }
    }

    fun getCurrentDirectoryName(): String? {
        if (!allowFolderNavigation) {
            return _uiState.value.currentPath
        }
        val path = _uiState.value.currentPath
        return when {
            // At the media-folder root the last path segment is "0"; show the source title (Images/Videos) instead.
            path == INTERNAL_STORAGE_PATH -> if (isMediaFolderBrowsing) title else null
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
            val startMs = System.currentTimeMillis()
            val results = filesToDelete.map { file ->
                async {
                    if (isRemote) {
                        fileMoveManager.deleteFile(file)
                    } else {
                        fileTrashManager.trashFile(file)
                    }
                }
            }.awaitAll()
            analyticsManager.trackFileDelete(
                sourceType = sourceType,
                isRemote = isRemote,
                itemCount = filesToDelete.size,
                permanent = isRemote,
                success = results.all { it },
                durationMs = System.currentTimeMillis() - startMs,
            )
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
            val startMs = System.currentTimeMillis()
            val success = fileMoveManager.renameFile(file, newName)
            analyticsManager.trackFileRename(
                sourceType = sourceType,
                isRemote = isRemote,
                fileType = file.fileType,
                success = success,
                durationMs = System.currentTimeMillis() - startMs,
            )
            _uiState.update { it.exitSelectionMode() }
            refreshFiles()
        }
    }

    fun createFolder(name: String) {
        val parentDir = _uiState.value.currentPath

        viewModelScope.launch(dispatcher) {
            val startMs = System.currentTimeMillis()
            val success = fileMoveManager.createFolder(parentDir, name)
            analyticsManager.trackFolderCreate(
                sourceType = sourceType,
                isRemote = isRemote,
                success = success,
                durationMs = System.currentTimeMillis() - startMs,
            )
            refreshFiles()
        }
    }

    fun cancelClipboard() {
        clipboardManager.clear()
    }

    fun pasteFiles() {
        val destDir = _uiState.value.currentPath
        // Snapshot the clipboard before paste() clears it, so item_count/size survive for telemetry.
        val clipboard = clipboardManager.state.value
        val operation = clipboard.operation

        viewModelScope.launch(dispatcher) {
            val startMs = System.currentTimeMillis()
            val success = clipboardManager.paste(destDir)
            if (operation != null && clipboard.files.isNotEmpty()) {
                analyticsManager.trackPaste(
                    operation = operation,
                    sourceType = sourceType,
                    isRemote = isRemote,
                    itemCount = clipboard.files.size,
                    sizeBytes = clipboard.files.sumOf { it.size },
                    success = success,
                    durationMs = System.currentTimeMillis() - startMs,
                )
            }
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
