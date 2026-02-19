package com.jonecx.ibex.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.jonecx.ibex.ui.theme.IbexTheme

fun ComposeContentTestRule.setIbexContent(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    setContent {
        IbexTheme(darkTheme = darkTheme) {
            content()
        }
    }
}
