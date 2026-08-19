package com.jonecx.ibex.util

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode

fun AndroidComposeTestRule<*, *>.runOnUiThreadBlocking(block: suspend () -> Unit) {
    runOnUiThread {
        kotlinx.coroutines.runBlocking { block() }
    }
}

// The home screen is a scrolling grid, so tiles past the first screenful (the Remote section, lower local
// tiles) aren't composed until scrolled to. Brings one into view by its label before asserting or tapping.
fun AndroidComposeTestRule<*, *>.scrollToHomeTile(label: String) {
    onNodeWithTag("home_source_grid").performScrollToNode(hasText(label, substring = true))
}

// Scrolls a home tile into view, then opens it.
fun AndroidComposeTestRule<*, *>.openHomeTile(label: String) {
    scrollToHomeTile(label)
    onNodeWithText(label).performClick()
    waitForIdle()
}
