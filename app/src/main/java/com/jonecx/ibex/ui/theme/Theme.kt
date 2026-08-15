package com.jonecx.ibex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Every accent and container role is pinned: Material3's baseline defaults are purple-tinted.
internal val LightColorScheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = Snow,
    // Accent container drives FABs and prompts; kept the single brand red, never a purple tonal.
    primaryContainer = BrandRed,
    onPrimaryContainer = Snow,
    // Destructive red darkened so it clears WCAG AA on light surfaces.
    error = BrandRedOnLight,
    onError = Snow,
    background = PaperLight,
    onBackground = Ink,
    surface = PaperLight,
    onSurface = Ink,
    surfaceVariant = MistLight,
    onSurfaceVariant = Slate,
    secondary = Slate,
    onSecondary = Snow,
    secondaryContainer = MistLight,
    onSecondaryContainer = Ink,
    tertiary = Slate,
    onTertiary = Snow,
    tertiaryContainer = MistLight,
    onTertiaryContainer = Ink,
    surfaceContainer = MistLight,
    surfaceContainerHigh = MistLight,
    surfaceContainerHighest = MistLight,
    outline = Steel,
    inversePrimary = BrandRed,
)

internal val DarkColorScheme = darkColorScheme(
    primary = BrandRed,
    onPrimary = Snow,
    // Accent container drives FABs and prompts; kept the single brand red, never a purple tonal.
    primaryContainer = BrandRed,
    onPrimaryContainer = Snow,
    // Lighter destructive red so small red text clears WCAG AA on dark surfaces.
    error = BrandRedOnDark,
    onError = Ink,
    background = PaperDark,
    onBackground = Snow,
    surface = PaperDark,
    onSurface = Snow,
    surfaceVariant = MistDark,
    onSurfaceVariant = Silver,
    secondary = Silver,
    onSecondary = Ink,
    secondaryContainer = MistDark,
    onSecondaryContainer = Snow,
    tertiary = Silver,
    onTertiary = Ink,
    tertiaryContainer = MistDark,
    onTertiaryContainer = Snow,
    // Bars and sheets stay pure black; only interactive containers get the grey.
    surfaceContainer = PaperDark,
    surfaceContainerHigh = MistDark,
    surfaceContainerHighest = MistDark,
    outline = Steel,
    inversePrimary = BrandRed,
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
