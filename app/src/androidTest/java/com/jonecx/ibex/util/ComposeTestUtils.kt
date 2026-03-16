package com.jonecx.ibex.util

import androidx.compose.ui.test.junit4.AndroidComposeTestRule

fun AndroidComposeTestRule<*, *>.runOnUiThreadBlocking(block: suspend () -> Unit) {
    runOnUiThread {
        kotlinx.coroutines.runBlocking { block() }
    }
}
