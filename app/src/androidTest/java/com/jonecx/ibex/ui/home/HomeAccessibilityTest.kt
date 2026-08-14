package com.jonecx.ibex.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.jonecx.ibex.data.model.FileSource
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.FileSources
import com.jonecx.ibex.ui.components.SourceTile
import com.jonecx.ibex.ui.theme.IbexTheme
import com.jonecx.ibex.ui.theme.SourceLiveColor
import com.jonecx.ibex.ui.theme.SourceStorageColor
import org.junit.Rule
import org.junit.Test

class HomeAccessibilityTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val storageSource = FileSource(
        id = "main_storage",
        name = "Storage",
        type = FileSourceType.LOCAL_STORAGE,
        icon = Icons.Filled.Storage,
        iconTint = SourceStorageColor,
        subtitle = "136.0 GB / 467.4 GB",
        contentDescription = "Storage, 136.0 GB used of 467.4 GB",
    )

    @Test
    fun tileExposesSingleSpokenContentDescription() {
        composeTestRule.setContent {
            IbexTheme { SourceTile(source = storageSource, onClick = {}) }
        }

        // Exactly one merged label, spelled out. Fails if the icon still duplicates the name.
        composeTestRule
            .onNodeWithContentDescription("Storage, 136.0 GB used of 467.4 GB")
            .assertContentDescriptionEquals("Storage, 136.0 GB used of 467.4 GB")
    }

    @Test
    fun tileIsAButtonWithClickAction() {
        composeTestRule.setContent {
            IbexTheme { SourceTile(source = storageSource, onClick = {}) }
        }

        composeTestRule
            .onNodeWithContentDescription("Storage, 136.0 GB used of 467.4 GB")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun decorativeIconIsNotSeparatelyLabelled() {
        composeTestRule.setContent {
            IbexTheme { SourceTile(source = storageSource, onClick = {}) }
        }

        // The icon must carry no label of its own, only the tile speaks.
        composeTestRule
            .onAllNodesWithContentDescription("Storage", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun disabledTileIsAnnouncedAsDisabled() {
        val disabled = FileSource(
            id = "live",
            name = "Live",
            type = FileSourceType.LIVE,
            icon = Icons.Filled.LiveTv,
            iconTint = SourceLiveColor,
            isEnabled = false,
        )
        composeTestRule.setContent {
            IbexTheme { SourceTile(source = disabled, onClick = {}) }
        }

        composeTestRule
            .onNodeWithContentDescription("Live")
            .assertIsNotEnabled()
    }

    @Test
    fun sectionHeadersAreHeadings() {
        val sources = FileSources.getRemoteSources("Cloud", "SMB/CIFS", "FTP", "Live")
        composeTestRule.setContent {
            IbexTheme {
                HomeScreenContent(
                    localSources = sources,
                    remoteSources = sources,
                    stats = emptyMap(),
                    storageUsage = null,
                    onSourceSelected = {},
                    onSettingsClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Local")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeTestRule.onNodeWithText("Remote")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}
