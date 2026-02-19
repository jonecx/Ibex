package com.jonecx.ibex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jonecx.ibex.ui.navigation.AppNavigation
import com.jonecx.ibex.ui.permission.PermissionScreen
import com.jonecx.ibex.ui.permission.checkStoragePermission
import com.jonecx.ibex.ui.theme.IbexTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IbexTheme {
                var hasPermission by remember { mutableStateOf(checkStoragePermission()) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (hasPermission) {
                        AppNavigation()
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
