package com.jonecx.ibex.ui.explorer

import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.repository.FileRepository
import com.jonecx.ibex.data.repository.MediaType
import com.jonecx.ibex.di.FileRepositoryFactory
import com.jonecx.ibex.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class FileExplorerUiState(
    val currentPath: String = Environment.getExternalStorageDirectory().absolutePath,
    val files: List<FileItem> = emptyList(),
    val selectedFile: FileItem? = null,
    val isLoading: Boolean = true,
    val error: Throwable? = null,
    val navigationStack: List<String> = listOf(Environment.getExternalStorageDirectory().absolutePath),
    val rootPath: String = Environment.getExternalStorageDirectory().absolutePath,
    val allowFolderNavigation: Boolean = true,
    val isAtInternalStorageRoot: Boolean = false,
)

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    private val repositoryFactory: FileRepositoryFactory,
    savedStateHandle: SavedStateHandle,
    @MainDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val sourceType: FileSourceType = FileSourceType.valueOf(
        savedStateHandle.get<String>("sourceType") ?: FileSourceType.LOCAL_STORAGE.name,
    )
    private val initialPath: String? = savedStateHandle.get<String>("rootPath")?.let {
        if (it.isNotEmpty()) URLDecoder.decode(it, "UTF-8") else null
    }
    private val title: String? = savedStateHandle.get<String>("title")?.let {
        if (it.isNotEmpty()) URLDecoder.decode(it, "UTF-8") else null
    }

    private val repository: FileRepository = createRepository(sourceType)
    private val allowFolderNavigation: Boolean = sourceType in listOf(
        FileSourceType.LOCAL_STORAGE,
        FileSourceType.LOCAL_DOWNLOADS,
    )
    private val startPath = initialPath ?: title ?: Environment.getExternalStorageDirectory().absolutePath

    private val _uiState = MutableStateFlow(
        FileExplorerUiState(
            currentPath = startPath,
            navigationStack = listOf(startPath),
            rootPath = startPath,
            allowFolderNavigation = allowFolderNavigation,
        ),
    )
    val uiState: StateFlow<FileExplorerUiState> = _uiState.asStateFlow()

    init {
        if (!allowFolderNavigation && title != null) {
            _uiState.value = _uiState.value.copy(currentPath = title)
        }
        loadFiles(startPath)
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
            else -> repositoryFactory.createLocalFileRepository()
        }
    }

    fun loadFiles(path: String) {
        viewModelScope.launch(dispatcher) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getFiles(path)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e,
                    )
                }
                .collect { files ->
                    val isAtRoot = path == Environment.getExternalStorageDirectory().absolutePath
                    _uiState.value = _uiState.value.copy(
                        currentPath = path,
                        files = files,
                        isLoading = false,
                        error = null,
                        isAtInternalStorageRoot = isAtRoot,
                    )
                }
        }
    }

    fun navigateTo(fileItem: FileItem) {
        if (fileItem.isDirectory && allowFolderNavigation) {
            val newStack = _uiState.value.navigationStack + fileItem.path
            _uiState.value = _uiState.value.copy(
                navigationStack = newStack,
                selectedFile = null,
            )
            loadFiles(fileItem.path)
        } else {
            _uiState.value = _uiState.value.copy(selectedFile = fileItem)
        }
    }

    fun navigateUp(): Boolean {
        val stack = _uiState.value.navigationStack
        if (stack.size > 1) {
            val newStack = stack.dropLast(1)
            val parentPath = newStack.last()
            _uiState.value = _uiState.value.copy(
                navigationStack = newStack,
                selectedFile = null,
            )
            loadFiles(parentPath)
            return true
        }
        return false
    }

    fun selectFile(fileItem: FileItem?) {
        _uiState.value = _uiState.value.copy(selectedFile = fileItem)
    }

    fun getCurrentDirectoryName(): String? {
        if (!allowFolderNavigation) {
            return _uiState.value.currentPath
        }
        val path = _uiState.value.currentPath
        return when {
            path == Environment.getExternalStorageDirectory().absolutePath -> null
            else -> path.substringAfterLast("/")
        }
    }

    fun setTitle(title: String) {
        _uiState.value = _uiState.value.copy(currentPath = title)
    }

    fun canNavigateUp(): Boolean {
        return _uiState.value.navigationStack.size > 1
    }
}
