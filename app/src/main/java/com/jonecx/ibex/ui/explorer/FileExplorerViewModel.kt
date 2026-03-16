package com.jonecx.ibex.ui.explorer

import android.os.Environment
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.ViewMode
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
) {
    val canCreateFolder: Boolean get() = allowFolderNavigation && !isRemoteBrowsing
}

val INTERNAL_STORAGE_PATH: String = Environment.getExternalStorageDirectory().absolutePath

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    private val repositoryFactory: FileRepositoryFactory,
    private val settingsPreferences: SettingsPreferencesContract,
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

    private var loadFilesJob: Job? = null

    init {
        if (!allowFolderNavigation && title != null) {
            _uiState.value = _uiState.value.copy(currentPath = title)
        }
        loadFiles(startPath)
        viewModelScope.launch(dispatcher) {
            settingsPreferences.viewMode.collect { mode ->
                _uiState.update { it.copy(viewMode = mode) }
            }
        }
        viewModelScope.launch(dispatcher) {
            settingsPreferences.gridColumns.collect { columns ->
                _uiState.update { it.copy(gridColumns = columns) }
            }
        }
        viewModelScope.launch(dispatcher) {
            clipboardManager.state.collect { clipboard ->
                _uiState.update { it.copy(clipboardOperation = clipboard.operation) }
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
                            files = files,
                            isLoading = false,
                            error = null,
                            isAtInternalStorageRoot = isAtRoot,
                        )
                    }
                }
        }
    }

    fun navigateTo(fileItem: FileItem) {
        if (fileItem.isDirectory && allowFolderNavigation) {
            val newStack = _uiState.value.navigationStack + fileItem.path
            _uiState.update {
                it.copy(
                    navigationStack = newStack,
                    selectedFile = null,
                )
            }
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
            _uiState.update {
                it.copy(
                    navigationStack = newStack,
                    selectedFile = null,
                )
            }
            loadFiles(parentPath)
            return true
        }
        return false
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
            filesToDelete.map { file -> async { fileTrashManager.trashFile(file) } }.awaitAll()
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
}

private fun FileExplorerUiState.exitSelectionMode() = copy(
    isSelectionMode = false,
    selectedFiles = emptySet(),
)

private fun FileExplorerUiState.selectedFileItems(): List<FileItem> =
    files.filter { it.path in selectedFiles }

private fun SavedStateHandle.decodedString(key: String): String? =
    get<String>(key)?.let { if (it.isNotEmpty()) URLDecoder.decode(it, "UTF-8") else null }
