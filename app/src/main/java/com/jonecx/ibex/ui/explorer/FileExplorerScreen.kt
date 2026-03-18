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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.SortOption
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.ui.components.ConfirmationDialog
import com.jonecx.ibex.ui.components.EmptyView
import com.jonecx.ibex.ui.components.ErrorView
import com.jonecx.ibex.ui.components.IbexTopAppBar
import com.jonecx.ibex.ui.components.LoadingView
import com.jonecx.ibex.ui.explorer.components.FileDetailPane
import com.jonecx.ibex.ui.explorer.components.FileGridItem
import com.jonecx.ibex.ui.explorer.components.FileListItem
import com.jonecx.ibex.ui.explorer.components.SortBottomSheet
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
                    onSaveScrollPosition = { index, offset ->
                        viewModel.saveScrollPosition(index, offset)
                    },
                    onFileClick = { fileItem ->
                        val state = viewModel.uiState.value
                        if (state.isSelectionMode) {
                            viewModel.toggleFileSelection(fileItem)
                        } else {
                            when {
                                fileItem.isDirectory -> viewModel.navigateTo(fileItem)

                                fileItem.fileType.isViewable -> {
                                    val viewableFiles = state.files.filter {
                                        it.fileType.isViewable
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
                        if (!uiState.isRemoteBrowsing) {
                            if (!viewModel.uiState.value.isSelectionMode) {
                                viewModel.enterSelectionMode(fileItem)
                            } else {
                                viewModel.toggleFileSelection(fileItem)
                            }
                        }
                    },
                    onCancelSelection = { viewModel.clearSelection() },
                    onDeleteSelected = { viewModel.deleteSelectedFiles() },
                    onMoveSelected = { viewModel.moveToClipboard() },
                    onCopySelected = { viewModel.copyToClipboard() },
                    onRenameSelected = { newName -> viewModel.renameSelectedFile(newName) },
                    onCreateFolder = { name -> viewModel.createFolder(name) },
                    onPaste = { viewModel.pasteFiles() },
                    onCancelClipboard = { viewModel.cancelClipboard() },
                    onSortOptionSelected = { viewModel.setSortOption(it) },
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
    onSaveScrollPosition: (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onCancelSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onRenameSelected: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onPaste: () -> Unit,
    onCancelClipboard: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onNavigateUp: () -> Unit,
    showBackButton: Boolean,
    currentDirectoryName: String,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    val saveCurrentScrollPosition = {
        val (index, offset) = when (uiState.viewMode) {
            ViewMode.LIST -> listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            ViewMode.GRID -> gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }
        onSaveScrollPosition(index, offset)
    }

    LaunchedEffect(uiState.currentPath, uiState.isLoading) {
        if (uiState.isLoading) return@LaunchedEffect
        val pos = uiState.restoredScrollPosition ?: ScrollPosition()
        when (uiState.viewMode) {
            ViewMode.LIST -> listState.scrollToItem(pos.firstVisibleItemIndex, pos.firstVisibleItemScrollOffset)
            ViewMode.GRID -> gridState.scrollToItem(pos.firstVisibleItemIndex, pos.firstVisibleItemScrollOffset)
        }
    }

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
                    actions = {
                        SortAction { showSortSheet = true }
                        CreateFolderAction(uiState.canCreateFolder) { showCreateFolderDialog = true }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            } else {
                IbexTopAppBar(
                    title = currentDirectoryName,
                    onNavigateBack = onNavigateUp,
                    showBackButton = showBackButton,
                    actions = {
                        SortAction { showSortSheet = true }
                        CreateFolderAction(uiState.canCreateFolder) { showCreateFolderDialog = true }
                    },
                )
            }
        },
        bottomBar = {
            when {
                uiState.isSelectionMode -> {
                    SelectionActionBar(
                        onCopy = onCopySelected,
                        onMove = onMoveSelected,
                        onRename = { showRenameDialog = true },
                        onDelete = { showDeleteDialog = true },
                        singleSelection = uiState.selectedFiles.size == 1,
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
                            state = listState,
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
                                    onClick = {
                                        if (fileItem.isDirectory) saveCurrentScrollPosition()
                                        onFileClick(fileItem)
                                    },
                                    isSelectionMode = uiState.isSelectionMode,
                                    isChecked = fileItem.path in uiState.selectedFiles,
                                    onLongClick = { onFileLongClick(fileItem) },
                                )
                            }
                        }
                    }
                    ViewMode.GRID -> {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(uiState.gridColumns),
                            modifier = Modifier.fillMaxSize().testTag("file_grid"),
                            contentPadding = contentPadding,
                            horizontalArrangement = Arrangement.spacedBy(0.5.dp),
                            verticalArrangement = Arrangement.spacedBy(0.5.dp),
                        ) {
                            items(
                                items = uiState.files,
                                key = { it.path },
                                contentType = { it.fileType },
                            ) { fileItem ->
                                FileGridItem(
                                    fileItem = fileItem,
                                    isSelected = selectedPath == fileItem.path,
                                    onClick = {
                                        if (fileItem.isDirectory) saveCurrentScrollPosition()
                                        onFileClick(fileItem)
                                    },
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

        if (showRenameDialog) {
            val currentName = uiState.files
                .firstOrNull { it.path in uiState.selectedFiles }?.name.orEmpty()
            TextInputDialog(
                title = stringResource(R.string.rename_dialog_title),
                hint = stringResource(R.string.rename_dialog_hint),
                confirmText = stringResource(R.string.rename_dialog_confirm),
                initialValue = currentName,
                isConfirmEnabled = { it.isNotBlank() && it != currentName },
                onConfirm = { newName ->
                    showRenameDialog = false
                    onRenameSelected(newName)
                },
                onDismiss = { showRenameDialog = false },
            )
        }

        if (showCreateFolderDialog) {
            TextInputDialog(
                title = stringResource(R.string.create_folder_dialog_title),
                hint = stringResource(R.string.create_folder_dialog_hint),
                confirmText = stringResource(R.string.create_folder_dialog_confirm),
                onConfirm = { name ->
                    showCreateFolderDialog = false
                    onCreateFolder(name)
                },
                onDismiss = { showCreateFolderDialog = false },
            )
        }

        if (showSortSheet) {
            SortBottomSheet(
                currentSortOption = uiState.sortOption,
                onSortOptionSelected = onSortOptionSelected,
                onDismiss = { showSortSheet = false },
            )
        }
    }
}

@Composable
private fun SortAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.FilterList,
            contentDescription = stringResource(R.string.sort),
        )
    }
}

@Composable
private fun CreateFolderAction(visible: Boolean, onClick: () -> Unit) {
    if (visible) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.CreateNewFolder,
                contentDescription = stringResource(R.string.create_folder),
            )
        }
    }
}

private data class ActionBarItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

@Composable
private fun SelectionActionBar(
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    singleSelection: Boolean,
) {
    EvenlySpacedActionBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        actions = listOf(
            ActionBarItem(Icons.Filled.ContentCopy, stringResource(R.string.copy), onCopy),
            ActionBarItem(Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.move), onMove),
            ActionBarItem(Icons.Filled.EditNote, stringResource(R.string.rename), onRename, enabled = singleSelection),
            ActionBarItem(Icons.Filled.Delete, stringResource(R.string.delete_selected), onDelete),
        ),
    )
}

@Composable
private fun ClipboardPasteBar(
    onCancel: () -> Unit,
    onPaste: () -> Unit,
) {
    EvenlySpacedActionBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        actions = listOf(
            ActionBarItem(Icons.Filled.Close, stringResource(R.string.cancel), onCancel),
            ActionBarItem(Icons.Filled.ContentPaste, stringResource(R.string.paste), onPaste),
        ),
    )
}

@Composable
private fun EvenlySpacedActionBar(
    containerColor: androidx.compose.ui.graphics.Color,
    actions: List<ActionBarItem>,
) {
    BottomAppBar(containerColor = containerColor) {
        actions.forEach { action ->
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = action.onClick, enabled = action.enabled) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    hint: String,
    confirmText: String,
    initialValue: String = "",
    isConfirmEnabled: (String) -> Boolean = { it.isNotBlank() },
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(text = hint) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = isConfirmEnabled(text),
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.delete_cancel))
            }
        },
    )
}
