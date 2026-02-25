package com.jonecx.ibex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.jonecx.ibex.ui.explorer.FileExplorerScreen
import com.jonecx.ibex.ui.explorer.FileExplorerViewModel
import com.jonecx.ibex.ui.home.HomeScreen
import com.jonecx.ibex.ui.settings.SettingsScreen
import com.jonecx.ibex.ui.viewer.ImageViewerArgs
import com.jonecx.ibex.ui.viewer.ImageViewerScreen
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val FILE_EXPLORER = "file_explorer/{sourceType}?rootPath={rootPath}&title={title}"
    const val IMAGE_VIEWER = "image_viewer"

    fun fileExplorer(sourceType: FileSourceType, rootPath: String? = null, title: String? = null): String {
        val encodedPath = rootPath?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        val encodedTitle = title?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        return "file_explorer/${sourceType.name}?rootPath=$encodedPath&title=$encodedTitle"
    }
}

@Composable
fun AppNavigation(
    analyticsManager: AnalyticsManager,
    imageViewerArgs: ImageViewerArgs,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
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
                            // TODO: Implement storage analysis screen
                        }
                        FileSourceType.CLOUD,
                        FileSourceType.SMB,
                        FileSourceType.FTP,
                        -> {
                            // TODO: Implement remote sources
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

        composable(
            route = Routes.FILE_EXPLORER,
            arguments = listOf(
                navArgument("sourceType") { type = NavType.StringType },
                navArgument("rootPath") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            val viewModel: FileExplorerViewModel = hiltViewModel()

            FileExplorerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenImageViewer = { viewableFiles, initialIndex ->
                    imageViewerArgs.set(viewableFiles, initialIndex)
                    navController.navigate(Routes.IMAGE_VIEWER)
                },
            )
        }

        composable(Routes.IMAGE_VIEWER) {
            ImageViewerScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
