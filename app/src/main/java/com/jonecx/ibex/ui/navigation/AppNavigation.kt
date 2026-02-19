package com.jonecx.ibex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.ui.explorer.FileExplorerScreen
import com.jonecx.ibex.ui.explorer.FileExplorerViewModel
import com.jonecx.ibex.ui.home.HomeScreen
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val FILE_EXPLORER = "file_explorer/{sourceType}?rootPath={rootPath}&title={title}"

    fun fileExplorer(sourceType: FileSourceType, rootPath: String? = null, title: String? = null): String {
        val encodedPath = rootPath?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        val encodedTitle = title?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        return "file_explorer/${sourceType.name}?rootPath=$encodedPath&title=$encodedTitle"
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onSourceSelected = { source ->
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
            )
        }
    }
}
