package com.jonecx.ibex.ui.explorer.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SelectionColors {
    // Neutral grey container: selection chrome stays monochrome, red is reserved for the action accents.
    val background: Color
        @Composable get() = MaterialTheme.colorScheme.secondaryContainer

    val defaultBackground: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val contentColor: Color
        @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer

    val defaultContentColor: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
}

@Composable
fun selectionBackgroundColor(isSelected: Boolean): Color =
    if (isSelected) SelectionColors.background else SelectionColors.defaultBackground

@Composable
fun selectionContentColor(isSelected: Boolean): Color =
    if (isSelected) SelectionColors.contentColor else SelectionColors.defaultContentColor
