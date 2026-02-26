package com.jonecx.ibex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.ui.explorer.components.FileImageRequestFactory
import com.jonecx.ibex.ui.explorer.components.LocalFileImageRequestFactory
import com.jonecx.ibex.ui.navigation.AppNavigation
import com.jonecx.ibex.ui.permission.PermissionChecker
import com.jonecx.ibex.ui.permission.PermissionScreen
import com.jonecx.ibex.ui.theme.IbexTheme
import com.jonecx.ibex.ui.viewer.MediaViewerArgs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionChecker: PermissionChecker

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var mediaViewerArgs: MediaViewerArgs

    @Inject
    lateinit var fileImageRequestFactory: FileImageRequestFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CompositionLocalProvider(LocalFileImageRequestFactory provides fileImageRequestFactory) {
                IbexTheme {
                    var hasPermission by remember { mutableStateOf(permissionChecker.hasStoragePermission()) }

                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (hasPermission) {
                            AppNavigation(
                                analyticsManager = analyticsManager,
                                mediaViewerArgs = mediaViewerArgs,
                            )
                        } else {
                            PermissionScreen(
                                onPermissionGranted = { hasPermission = true },
                            )
                        }
                    }
                }
            }
        }
    }
}
