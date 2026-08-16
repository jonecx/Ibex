package com.jonecx.ibex.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonecx.ibex.ui.theme.IbexTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TransferConflictDialogTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun showsThreeChoicesAndReportsTheChosenOne() {
        var choice = ""
        composeTestRule.setContent {
            IbexTheme {
                TransferConflictDialog(
                    conflictCount = 2,
                    sampleName = "IMG_2043.mp4",
                    onKeepBoth = { choice = "keep_both" },
                    onOverwrite = { choice = "overwrite" },
                    onSkip = { choice = "skip" },
                    onDismiss = { choice = "dismiss" },
                )
            }
        }

        composeTestRule.onNodeWithText("Keep both").assertIsDisplayed()
        composeTestRule.onNodeWithText("Overwrite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip").assertIsDisplayed()

        composeTestRule.onNodeWithText("Overwrite").performClick()
        assertEquals("overwrite", choice)
    }
}
