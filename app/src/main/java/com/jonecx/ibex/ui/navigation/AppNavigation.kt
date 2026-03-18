package com.jonecx.ibex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.NetworkProtocol
import com.jonecx.ibex.ui.analysis.StorageAnalysisScreen
import com.jonecx.ibex.ui.explorer.FileExplorerScreen
import com.jonecx.ibex.ui.explorer.FileExplorerViewModel
import com.jonecx.ibex.ui.home.HomeScreen
import com.jonecx.ibex.ui.network.AddNetworkConnectionScreen
import com.jonecx.ibex.ui.network.NetworkConnectionsScreen
import com.jonecx.ibex.ui.network.NetworkConnectionsViewModel
import com.jonecx.ibex.ui.settings.SettingsScreen
import com.jonecx.ibex.ui.viewer.LocalMediaViewerArgs
import com.jonecx.ibex.ui.viewer.MediaViewerScreen
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val STORAGE_ANALYSIS = "storage_analysis"
    private const val NETWORK_CONNECTIONS_BASE = "network_connections"
    const val NETWORK_CONNECTIONS = "$NETWORK_CONNECTIONS_BASE?${NetworkConnectionsViewModel.ARG_PROTOCOL}={${NetworkConnectionsViewModel.ARG_PROTOCOL}}"
    const val ADD_NETWORK_CONNECTION = "add_network_connection"

    fun networkConnections(protocol: NetworkProtocol): String =
        "$NETWORK_CONNECTIONS_BASE?${NetworkConnectionsViewModel.ARG_PROTOCOL}=${protocol.name}"
    const val FILE_EXPLORER = "file_explorer/{${FileExplorerViewModel.ARG_SOURCE_TYPE}}?${FileExplorerViewModel.ARG_ROOT_PATH}={${FileExplorerViewModel.ARG_ROOT_PATH}}&${FileExplorerViewModel.ARG_TITLE}={${FileExplorerViewModel.ARG_TITLE}}&${FileExplorerViewModel.ARG_CONNECTION_ID}={${FileExplorerViewModel.ARG_CONNECTION_ID}}"
    const val MEDIA_VIEWER = "media_viewer"
    const val KEY_REFRESH = "refresh"

    fun fileExplorer(
        sourceType: FileSourceType,
        rootPath: String? = null,
        title: String? = null,
        connectionId: String? = null,
    ): String {
        val encodedPath = rootPath?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        val encodedTitle = title?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        val encodedConnectionId = connectionId?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        return "file_explorer/${sourceType.name}?${FileExplorerViewModel.ARG_ROOT_PATH}=$encodedPath&${FileExplorerViewModel.ARG_TITLE}=$encodedTitle&${FileExplorerViewModel.ARG_CONNECTION_ID}=$encodedConnectionId"
    }
}

@Composable
fun AppNavigation(
    analyticsManager: AnalyticsManager,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val mediaViewerArgs = LocalMediaViewerArgs.current
    var currentScreen by remember { mutableStateOf("") }
    var screenEntryTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val newScreen = destination.route?.substringBefore("/") ?: "unknown"

            if (currentScreen.isNotEmpty() && currentScreen != newScreen) {
                val duration = System.currentTimeMillis() - screenEntryTime
                analyticsManager.trackScreenExit(currentScreen, duration)
            }

            currentScreen = newScreen
            screenEntryTime = System.currentTimeMillis()
            analyticsManager.trackScreenView(newScreen)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onSourceSelected = { source ->
                    analyticsManager.trackTileClick(source.name, source.id)
                    when (source.type) {
                        FileSourceType.LOCAL_STORAGE,
                        FileSourceType.LOCAL_DOWNLOADS,
                        -> {
                            navController.navigate(Routes.fileExplorer(source.type, source.rootPath, source.name))
                        }
                        FileSourceType.LOCAL_IMAGES,
                        FileSourceType.LOCAL_VIDEOS,
                        FileSourceType.LOCAL_AUDIO,
                        FileSourceType.LOCAL_DOCUMENTS,
                        FileSourceType.LOCAL_APPS,
                        FileSourceType.LOCAL_RECENT,
                        FileSourceType.LOCAL_TRASH,
                        -> {
                            navController.navigate(Routes.fileExplorer(source.type, null, source.name))
                        }
                        FileSourceType.STORAGE_ANALYSIS -> {
                            navController.navigate(Routes.STORAGE_ANALYSIS)
                        }
                        FileSourceType.SMB,
                        FileSourceType.FTP,
                        FileSourceType.CLOUD,
                        -> {
                            val protocol = NetworkProtocol.valueOf(source.type.name)
                            navController.navigate(Routes.networkConnections(protocol))
                        }
                    }
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Routes.STORAGE_ANALYSIS) {
            StorageAnalysisScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.FILE_EXPLORER,
            arguments = listOf(
                navArgument(FileExplorerViewModel.ARG_SOURCE_TYPE) { type = NavType.StringType },
                navArgument(FileExplorerViewModel.ARG_ROOT_PATH) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(FileExplorerViewModel.ARG_TITLE) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(FileExplorerViewModel.ARG_CONNECTION_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            val viewModel: FileExplorerViewModel = hiltViewModel()

            val shouldRefresh by it.savedStateHandle
                .getStateFlow(Routes.KEY_REFRESH, false)
                .collectAsState()

            LaunchedEffect(shouldRefresh) {
                if (shouldRefresh) {
                    viewModel.refreshFiles()
                    it.savedStateHandle[Routes.KEY_REFRESH] = false
                }
            }

            FileExplorerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenMediaViewer = { viewableFiles, initialIndex ->
                    mediaViewerArgs.set(viewableFiles, initialIndex)
                    navController.navigate(Routes.MEDIA_VIEWER)
                },
            )
        }

        composable(
            route = Routes.NETWORK_CONNECTIONS,
            arguments = listOf(
                navArgument(NetworkConnectionsViewModel.ARG_PROTOCOL) {
                    type = NavType.StringType
                    defaultValue = NetworkProtocol.SMB.name
                },
            ),
        ) {
            val viewModel: NetworkConnectionsViewModel = hiltViewModel()
            NetworkConnectionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onConnectionSelected = { connection ->
                    navController.navigate(
                        Routes.fileExplorer(
                            sourceType = FileSourceType.SMB,
                            title = connection.displayName,
                            connectionId = connection.id,
                        ),
                    )
                },
                onAddConnection = {
                    viewModel.clearConnectionToEdit()
                    navController.navigate(Routes.ADD_NETWORK_CONNECTION)
                },
                onEditConnection = { connection ->
                    viewModel.setConnectionToEdit(connection)
                    navController.navigate(Routes.ADD_NETWORK_CONNECTION)
                },
                viewModel = viewModel,
            )
        }

        composable(Routes.ADD_NETWORK_CONNECTION) {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Routes.NETWORK_CONNECTIONS)
            }
            val viewModel: NetworkConnectionsViewModel = hiltViewModel(parentEntry)
            val uiState = viewModel.uiState.collectAsState().value
            AddNetworkConnectionScreen(
                onNavigateBack = {
                    viewModel.clearConnectionToEdit()
                    navController.popBackStack()
                },
                onSave = { connection ->
                    if (uiState.connectionToEdit != null) {
                        viewModel.updateConnection(connection)
                    } else {
                        viewModel.addConnection(connection)
                    }
                    viewModel.clearConnectionToEdit()
                    navController.popBackStack()
                },
                defaultProtocol = uiState.defaultProtocol,
                connectionToEdit = uiState.connectionToEdit,
            )
        }

        composable(Routes.MEDIA_VIEWER) {
            MediaViewerScreen(
                onNavigateBack = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(Routes.KEY_REFRESH, true)
                    navController.popBackStack()
                },
            )
        }
    }
}
