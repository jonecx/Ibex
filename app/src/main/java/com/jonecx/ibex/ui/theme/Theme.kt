package com.jonecx.ibex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// NYT-style minimalist dark theme
private val DarkColorScheme = darkColorScheme(
    primary = White,
    secondary = GrayDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = Black,
    onSecondary = Black,
    onBackground = White,
    onSurface = White,
    onSurfaceVariant = GrayDark,
)

// NYT-style minimalist light theme
private val LightColorScheme = lightColorScheme(
    primary = Black,
    secondary = GrayLight,
    background = White,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = White,
    onSecondary = White,
    onBackground = Black,
    onSurface = Black,
    onSurfaceVariant = GrayLight,
)

@Composable
fun IbexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
