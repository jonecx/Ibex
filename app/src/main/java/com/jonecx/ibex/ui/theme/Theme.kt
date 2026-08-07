package com.jonecx.ibex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// The neutral roles are pinned too: Material3's baseline containers are purple-tinted.
private val LightColorScheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = Snow,
    background = PaperLight,
    onBackground = Ink,
    surface = PaperLight,
    onSurface = Ink,
    surfaceVariant = MistLight,
    onSurfaceVariant = Slate,
    secondaryContainer = MistLight,
    onSecondaryContainer = Ink,
    surfaceContainer = MistLight,
    surfaceContainerHigh = MistLight,
    surfaceContainerHighest = MistLight,
    outline = Steel,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandRed,
    onPrimary = Snow,
    background = PaperDark,
    onBackground = Snow,
    surface = PaperDark,
    onSurface = Snow,
    surfaceVariant = MistDark,
    onSurfaceVariant = Silver,
    secondaryContainer = MistDark,
    onSecondaryContainer = Snow,
    // Bars and sheets stay pure black; only interactive containers get the grey.
    surfaceContainer = PaperDark,
    surfaceContainerHigh = MistDark,
    surfaceContainerHighest = MistDark,
    outline = Steel,
)

@Composable
fun IbexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
