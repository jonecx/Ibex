package com.jonecx.ibex.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.jonecx.ibex.ui.theme.IbexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FastScrollerTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val letters = ('A'..'Z').map { it.toString() }

    @Test
    fun draggingRailScrubsToAnIndex() {
        var scrolledTo = -1
        composeTestRule.setContent {
            IbexTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    FastScrollerContent(
                        totalItems = letters.size,
                        firstVisibleIndex = 0,
                        visibleCount = 6,
                        canScroll = true,
                        isScrolling = false,
                        labelForIndex = { letters[it] },
                        onScrollToIndex = { scrolledTo = it },
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Fast scroll").performTouchInput { swipeDown() }

        assertTrue("expected a scrub past the top", scrolledTo > 0)
    }

    @Test
    fun railIsInertWhenNothingScrolls() {
        var scrolledTo = -1
        composeTestRule.setContent {
            IbexTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    FastScrollerContent(
                        totalItems = letters.size,
                        firstVisibleIndex = 0,
                        visibleCount = letters.size,
                        canScroll = false,
                        isScrolling = false,
                        labelForIndex = { letters[it] },
                        onScrollToIndex = { scrolledTo = it },
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Fast scroll").performTouchInput { swipeDown() }

        assertEquals(-1, scrolledTo)
    }

    @Test
    fun bubbleShowsTheSectionLetterWhileDragging() {
        composeTestRule.setContent {
            IbexTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Pin the rail to the end edge as production does; the bubble sits to its
                    // left, so a start-aligned rail would push the bubble off-screen.
                    FastScrollOverlay(
                        alpha = 1f,
                        thumbFraction = 0.4f,
                        dragging = true,
                        bubbleFraction = 0.4f,
                        label = "F",
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("F").assertIsDisplayed()
    }
}
