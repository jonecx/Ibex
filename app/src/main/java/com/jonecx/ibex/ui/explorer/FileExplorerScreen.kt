package com.jonecx.ibex.ui.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.ui.components.ConfirmationDialog
import com.jonecx.ibex.ui.components.EmptyView
import com.jonecx.ibex.ui.components.ErrorView
import com.jonecx.ibex.ui.components.LoadingView
import com.jonecx.ibex.ui.explorer.components.FileDetailPane
import com.jonecx.ibex.ui.explorer.components.FileGridItem
import com.jonecx.ibex.ui.explorer.components.FileListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    viewModel: FileExplorerViewModel,
    onNavigateBack: () -> Unit,
    onOpenMediaViewer: (viewableFiles: List<FileItem>, initialIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        scope.launch {
            when {
                uiState.isSelectionMode -> viewModel.clearSelection()
                navigator.canNavigateBack() -> navigator.navigateBack()
                viewModel.canNavigateUp() -> viewModel.navigateUp()
                else -> onNavigateBack()
            }
        }
    }

    ListDetailPaneScaffold(
        modifier = modifier,
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                FileListPane(
                    uiState = uiState,
                    onFileClick = { fileItem ->
                        if (uiState.isSelectionMode) {
                            viewModel.toggleFileSelection(fileItem)
                        } else {
                            when (fileItem.fileType) {
                                FileType.DIRECTORY -> viewModel.navigateTo(fileItem)

                                FileType.IMAGE, FileType.VIDEO -> {
                                    val viewableFiles = uiState.files.filter {
                                        it.fileType == FileType.IMAGE || it.fileType == FileType.VIDEO
                                    }
                                    val index = viewableFiles.indexOfFirst { it.path == fileItem.path }
                                    if (index >= 0) {
                                        onOpenMediaViewer(viewableFiles, index)
                                    }
                                }

                                else -> {
                                    viewModel.selectFile(fileItem)
                                    scope.launch {
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    }
                                }
                            }
                        }
                    },
                    onFileLongClick = { fileItem ->
                        if (!uiState.isSelectionMode) {
                            viewModel.enterSelectionMode(fileItem)
                        } else {
                            viewModel.toggleFileSelection(fileItem)
                        }
                    },
                    onCancelSelection = { viewModel.clearSelection() },
                    onDeleteSelected = { viewModel.deleteSelectedFiles() },
                    onMoveSelected = { viewModel.moveToClipboard() },
                    onCopySelected = { viewModel.copyToClipboard() },
                    onPaste = { viewModel.pasteFiles() },
                    onCancelClipboard = { viewModel.cancelClipboard() },
                    onNavigateUp = {
                        if (viewModel.canNavigateUp()) {
                            viewModel.navigateUp()
                        } else {
                            onNavigateBack()
                        }
                    },
                    showBackButton = true,
                    currentDirectoryName = viewModel.getCurrentDirectoryName() ?: stringResource(R.string.internal_storage),
                )
            }
        },
        detailPane = {
            AnimatedPane {
                FileDetailPane(
                    fileItem = uiState.selectedFile,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileListPane(
    uiState: FileExplorerUiState,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onCancelSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onPaste: () -> Unit,
    onCancelClipboard: () -> Unit,
    onNavigateUp: () -> Unit,
    showBackButton: Boolean,
    currentDirectoryName: String,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.selected_count, uiState.selectedFiles.size),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onCancelSelection) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cancel_selection),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = currentDirectoryName,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.navigate_up),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
        bottomBar = {
            when {
                uiState.isSelectionMode -> {
                    SelectionActionBar(
                        onCopy = onCopySelected,
                        onMove = onMoveSelected,
                        onDelete = { showDeleteDialog = true },
                    )
                }
                uiState.clipboardOperation != null -> {
                    ClipboardPasteBar(
                        onCancel = onCancelClipboard,
                        onPaste = onPaste,
                    )
                }
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingView(
                    modifier = Modifier.padding(paddingValues),
                )
            }
            uiState.error != null -> {
                ErrorView(
                    message = uiState.error.message,
                    modifier = Modifier.padding(paddingValues),
                )
            }
            uiState.files.isEmpty() -> {
                EmptyView(
                    modifier = Modifier.padding(paddingValues),
                )
            }
            else -> {
                val contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                )
                val selectedPath = uiState.selectedFile?.path
                when (uiState.viewMode) {
                    ViewMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("file_list"),
                            contentPadding = contentPadding,
                        ) {
                            items(
                                items = uiState.files,
                                key = { it.path },
                                contentType = { it.fileType },
                            ) { fileItem ->
                                FileListItem(
                                    fileItem = fileItem,
                                    isSelected = selectedPath == fileItem.path,
                                    onClick = { onFileClick(fileItem) },
                                    isSelectionMode = uiState.isSelectionMode,
                                    isChecked = fileItem.path in uiState.selectedFiles,
                                    onLongClick = { onFileLongClick(fileItem) },
                                )
                            }
                        }
                    }
                    ViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            modifier = Modifier.fillMaxSize().testTag("file_grid"),
                            contentPadding = contentPadding,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = uiState.files,
                                key = { it.path },
                                contentType = { it.fileType },
                            ) { fileItem ->
                                FileGridItem(
                                    fileItem = fileItem,
                                    isSelected = selectedPath == fileItem.path,
                                    onClick = { onFileClick(fileItem) },
                                    isSelectionMode = uiState.isSelectionMode,
                                    isChecked = fileItem.path in uiState.selectedFiles,
                                    onLongClick = { onFileLongClick(fileItem) },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            ConfirmationDialog(
                title = stringResource(R.string.delete_selected_title, uiState.selectedFiles.size),
                message = stringResource(R.string.delete_selected_message),
                confirmText = stringResource(R.string.delete_confirm),
                dismissText = stringResource(R.string.delete_cancel),
                onConfirm = {
                    showDeleteDialog = false
                    onDeleteSelected()
                },
                onDismiss = { showDeleteDialog = false },
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        ActionBarButton(
            icon = Icons.Filled.ContentCopy,
            label = stringResource(R.string.copy),
            onClick = onCopy,
        )
        Spacer(modifier = Modifier.weight(1f))
        ActionBarButton(
            icon = Icons.Filled.DriveFileMove,
            label = stringResource(R.string.move),
            onClick = onMove,
        )
        Spacer(modifier = Modifier.weight(1f))
        ActionBarButton(
            icon = Icons.Filled.Delete,
            label = stringResource(R.string.delete_selected),
            onClick = onDelete,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ClipboardPasteBar(
    onCancel: () -> Unit,
    onPaste: () -> Unit,
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        ActionBarButton(
            icon = Icons.Filled.Close,
            label = stringResource(R.string.cancel),
            onClick = onCancel,
        )
        Spacer(modifier = Modifier.weight(1f))
        ActionBarButton(
            icon = Icons.Filled.ContentPaste,
            label = stringResource(R.string.paste),
            onClick = onPaste,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActionBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
