package com.jonecx.ibex.ui.viewer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.repository.FileTrashManager
import com.jonecx.ibex.data.repository.SmbContextProvider
import com.jonecx.ibex.di.IoDispatcher
import com.jonecx.ibex.ui.player.PlayerFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class MediaViewerUiState(
    val viewableFiles: List<FileItem> = emptyList(),
    val initialIndex: Int = 0,
    val downloadingPaths: Set<String> = emptySet(),
    val resolvedFiles: Map<String, FileItem> = emptyMap(),
)

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val mediaViewerArgs: MediaViewerArgs,
    private val playerFactory: PlayerFactory,
    private val fileTrashManager: FileTrashManager,
    private val smbContextProvider: SmbContextProvider,
    @ApplicationContext private val appContext: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MediaViewerUiState(
            viewableFiles = mediaViewerArgs.viewableFiles,
            initialIndex = mediaViewerArgs.initialIndex,
        ),
    )
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()

    fun downloadRemoteVideo(fileItem: FileItem) {
        if (!fileItem.isRemote) return
        if (_uiState.value.downloadingPaths.contains(fileItem.path)) return
        if (_uiState.value.resolvedFiles.containsKey(fileItem.path)) return

        _uiState.update { it.copy(downloadingPaths = it.downloadingPaths + fileItem.path) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val host = Uri.parse(fileItem.path).host ?: return@launch
                val cifsContext = smbContextProvider.get(host) ?: return@launch
                val ext = fileItem.name.substringAfterLast('.', "")
                val tempFile = File(
                    appContext.cacheDir,
                    "smb_media_${SmbContextProvider.smbCacheKey(fileItem.path)}.$ext",
                )

                SmbFile(fileItem.path, cifsContext).inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output, bufferSize = DOWNLOAD_BUFFER_SIZE)
                    }
                }

                val localFileItem = fileItem.copy(
                    path = tempFile.absolutePath,
                    uri = Uri.fromFile(tempFile),
                    isRemote = false,
                )
                _uiState.update { state ->
                    state.copy(
                        downloadingPaths = state.downloadingPaths - fileItem.path,
                        resolvedFiles = state.resolvedFiles + (fileItem.path to localFileItem),
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(downloadingPaths = it.downloadingPaths - fileItem.path) }
            }
        }
    }

    fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch {
            val trashed = fileTrashManager.trashFile(fileItem)
            if (trashed) {
                _uiState.update { state ->
                    val updatedFiles = state.viewableFiles.filterNot { it.path == fileItem.path }
                    state.copy(viewableFiles = updatedFiles)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaViewerArgs.clear()
    }

    companion object {
        private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
    }
}
