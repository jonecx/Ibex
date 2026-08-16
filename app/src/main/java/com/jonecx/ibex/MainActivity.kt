package com.jonecx.ibex

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jonecx.azmaree.image.LocalAzmareeImageEngine
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.data.model.ThemeMode
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.data.transfer.TransferManager
import com.jonecx.ibex.ui.explorer.components.CoilImageEngine
import com.jonecx.ibex.ui.explorer.components.FileImageRequestFactory
import com.jonecx.ibex.ui.explorer.components.LocalFileImageRequestFactory
import com.jonecx.ibex.ui.navigation.AppNavigation
import com.jonecx.ibex.ui.permission.PermissionChecker
import com.jonecx.ibex.ui.permission.PermissionScreen
import com.jonecx.ibex.ui.theme.IbexTheme
import com.jonecx.ibex.ui.viewer.LocalMediaViewerArgs
import com.jonecx.ibex.ui.viewer.MediaViewerArgs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val permissionChecker: PermissionChecker by inject()

    private val analyticsManager: AnalyticsManager by inject()

    private val mediaViewerArgs: MediaViewerArgs by inject()

    private val fileImageRequestFactory: FileImageRequestFactory by inject()

    private val settingsPreferences: SettingsPreferencesContract by inject()

    private val transferManager: TransferManager by inject()

    // Registered up front (before the activity is STARTED) so the launcher is always valid, unlike a
    // launcher created lazily in composition.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionOnFirstTransfer()

        setContent {
            val imageEngine = remember { CoilImageEngine() }
            val themeMode by settingsPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Keep the status/nav bar icons legible against the chosen theme, re-running on config
            // changes (which otherwise re-derive the bar style from the window theme).
            val configuration = LocalConfiguration.current
            DisposableEffect(darkTheme, configuration) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            CompositionLocalProvider(
                LocalFileImageRequestFactory provides fileImageRequestFactory,
                LocalAzmareeImageEngine provides imageEngine,
                LocalMediaViewerArgs provides mediaViewerArgs,
            ) {
                IbexTheme(darkTheme = darkTheme) {
                    var hasPermission by remember { mutableStateOf(permissionChecker.hasStoragePermission()) }

                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (hasPermission) {
                            AppNavigation(
                                analyticsManager = analyticsManager,
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

    // Ask for POST_NOTIFICATIONS (API 33+) the first time a transfer is actually running, so its ongoing
    // progress notification can show. Contextual and one-shot; the system suppresses it after the user decides.
    private fun requestNotificationPermissionOnFirstTransfer() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        lifecycleScope.launch {
            // Only when work is actually running/queued (i.e. the notification will show), not for a
            // reloaded paused or failed job.
            transferManager.snapshot.first { it.hasRunningOrQueued }
            val granted = ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
