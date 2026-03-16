package com.jonecx.ibex.ui.explorer.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SelectionColors {
    val background: Color
        @Composable get() = MaterialTheme.colorScheme.primaryContainer

    val defaultBackground: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val contentColor: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer

    val defaultContentColor: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
}

@Composable
fun selectionBackgroundColor(isSelected: Boolean): Color =
    if (isSelected) SelectionColors.background else SelectionColors.defaultBackground

@Composable
fun selectionContentColor(isSelected: Boolean): Color =
    if (isSelected) SelectionColors.contentColor else SelectionColors.defaultContentColor
