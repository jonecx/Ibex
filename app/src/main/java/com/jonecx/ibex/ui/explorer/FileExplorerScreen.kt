package com.jonecx.ibex.ui.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.ui.components.EmptyView
import com.jonecx.ibex.ui.components.ErrorView
import com.jonecx.ibex.ui.components.LoadingView
import com.jonecx.ibex.ui.explorer.components.FileDetailPane
import com.jonecx.ibex.ui.explorer.components.FileListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    viewModel: FileExplorerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        scope.launch {
            when {
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
                        if (fileItem.isDirectory) {
                            viewModel.navigateTo(fileItem)
                        } else {
                            viewModel.selectFile(fileItem)
                            scope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                            }
                        }
                    },
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
    onNavigateUp: () -> Unit,
    showBackButton: Boolean,
    currentDirectoryName: String,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        bottom = 16.dp,
                    ),
                ) {
                    items(
                        items = uiState.files,
                        key = { it.path },
                    ) { fileItem ->
                        FileListItem(
                            fileItem = fileItem,
                            isSelected = uiState.selectedFile?.path == fileItem.path,
                            onClick = { onFileClick(fileItem) },
                        )
                    }
                }
            }
        }
    }
}
