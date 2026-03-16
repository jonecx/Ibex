package com.jonecx.ibex.ui.player

import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.ui.explorer.components.MediaViewerOverlay
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.inject.Inject

private const val TEST_VIDEO_ASSET_1 = "earth_MP4_480_1_5MG.mp4"
private const val TEST_VIDEO_ASSET_2 = "15764815-hd_1080_1920_60fps.mp4"
private const val PLAYBACK_TIMEOUT_MS = 5_000L

@HiltAndroidTest
class VideoPlayerIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Uses createComposeRule() instead of createAndroidComposeRule<MainActivity>() because
    // video player tests require setContent() to render composables in isolation with precise
    // control over player state, clock, and navigation callbacks.
    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Inject
    lateinit var playerFactory: PlayerFactory

    private lateinit var videoFileItems: List<FileItem>

    @Before
    fun setup() {
        hiltRule.inject()

        videoFileItems = listOf(TEST_VIDEO_ASSET_1, TEST_VIDEO_ASSET_2).map { assetName ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val videoFile = File(instrumentation.targetContext.cacheDir, assetName)
            if (!videoFile.exists()) {
                instrumentation.context.assets.open(assetName).use { input ->
                    videoFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            FileItem(
                name = assetName,
                path = videoFile.absolutePath,
                uri = Uri.fromFile(videoFile),
                size = videoFile.length(),
                lastModified = videoFile.lastModified(),
                isDirectory = false,
                fileType = FileType.VIDEO,
            )
        }
    }

    private fun setVideoPlayerContent(
        isActive: Boolean,
        onPrevious: (() -> Unit)? = null,
        onNext: (() -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalPlayerFactory provides playerFactory) {
                VideoPlayer(
                    fileItem = videoFileItems.first(),
                    isActive = isActive,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
            }
        }
    }

    private fun setMediaViewerContent(
        initialIndex: Int = 0,
        viewableFiles: List<FileItem> = videoFileItems,
        onDelete: (FileItem) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalPlayerFactory provides playerFactory) {
                MediaViewerOverlay(
                    viewableFiles = viewableFiles,
                    initialIndex = initialIndex,
                    onDismiss = {},
                    onDelete = onDelete,
                )
            }
        }
    }

    private fun ComposeContentTestRule.awaitNode(
        description: String,
        substring: Boolean = false,
    ) {
        waitUntil(timeoutMillis = PLAYBACK_TIMEOUT_MS) {
            onAllNodes(hasContentDescription(description, substring = substring))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun ComposeContentTestRule.awaitText(text: String) {
        waitUntil(timeoutMillis = PLAYBACK_TIMEOUT_MS) {
            onAllNodes(hasText(text))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun openDeleteDialog() {
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
    }

    private fun confirmDelete() {
        composeTestRule.onAllNodes(hasText("Delete"))
            .filterToOne(!hasContentDescription("Delete"))
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun setVideoPlayerContentFrozen() {
        composeTestRule.mainClock.autoAdvance = false
        setVideoPlayerContent(isActive = false)
        composeTestRule.mainClock.advanceTimeByFrame()
    }

    private fun openSpeedMenu() {
        setVideoPlayerContent(isActive = false)
        composeTestRule.onNodeWithContentDescription("Playback speed").performClick()
    }

    @Test
    fun loadingIndicatorShownWhileBuffering() {
        setVideoPlayerContentFrozen()

        composeTestRule.onNodeWithContentDescription("Loading video").assertIsDisplayed()
    }

    @Test
    fun loadingIndicatorHiddenWhenReady() {
        setVideoPlayerContent(isActive = true)

        composeTestRule.waitUntil(timeoutMillis = PLAYBACK_TIMEOUT_MS) {
            composeTestRule.onAllNodes(hasContentDescription("Loading video"))
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    @Test
    fun controlsAreDisplayed() {
        setVideoPlayerContent(isActive = false)

        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Skip back", substring = true))
            .assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Skip forward", substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun videoPlaysWhenActive() {
        setVideoPlayerContent(isActive = true)

        composeTestRule.awaitNode("Pause")
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun tapPlayStartsPlayback() {
        setVideoPlayerContent(isActive = false)

        composeTestRule.onNodeWithContentDescription("Play").performClick()

        composeTestRule.awaitNode("Pause")
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun tapPausePausesPlayback() {
        setVideoPlayerContent(isActive = true)

        composeTestRule.awaitNode("Pause")
        composeTestRule.onNodeWithContentDescription("Pause").performClick()

        composeTestRule.awaitNode("Play")
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun timeLabelsDisplayedWhenInactive() {
        setVideoPlayerContentFrozen()

        val nodes = composeTestRule.onAllNodes(hasText("0:00"))
        nodes.assertCountEquals(2)
    }

    @Test
    fun durationUpdatesWhenPlaying() {
        setVideoPlayerContent(isActive = true)

        composeTestRule.awaitNode("Pause")
        composeTestRule.waitUntil(timeoutMillis = PLAYBACK_TIMEOUT_MS) {
            composeTestRule.onAllNodes(hasText("0:00"))
                .fetchSemanticsNodes()
                .size < 2
        }
    }

    @Test
    fun previousAndNextButtonsDisplayedWhenCallbacksProvided() {
        setVideoPlayerContent(
            isActive = false,
            onPrevious = {},
            onNext = {},
        )

        composeTestRule.onNodeWithContentDescription("Previous").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Next").assertIsDisplayed()
    }

    @Test
    fun previousButtonDisabledWhenNoPreviousCallback() {
        setVideoPlayerContent(
            isActive = false,
            onPrevious = null,
            onNext = {},
        )

        composeTestRule.onNodeWithContentDescription("Previous").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Next").assertIsEnabled()
    }

    @Test
    fun nextButtonDisabledWhenNoNextCallback() {
        setVideoPlayerContent(
            isActive = false,
            onPrevious = {},
            onNext = null,
        )

        composeTestRule.onNodeWithContentDescription("Previous").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Next").assertIsNotEnabled()
    }

    @Test
    fun firstVideoHasPreviousDisabledAndNextEnabled() {
        setMediaViewerContent(initialIndex = 0)

        composeTestRule.onNodeWithContentDescription("Previous").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Next").assertIsEnabled()
    }

    @Test
    fun lastVideoHasPreviousEnabledAndNextDisabled() {
        setMediaViewerContent(initialIndex = 1)

        composeTestRule.onNodeWithContentDescription("Previous").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Next").assertIsNotEnabled()
    }

    @Test
    fun tapNextNavigatesToSecondVideo() {
        setMediaViewerContent(initialIndex = 0)

        composeTestRule.awaitText("1 / 2")
        composeTestRule.onNodeWithContentDescription("Next").performClick()

        composeTestRule.awaitText("2 / 2")
        composeTestRule.onNodeWithText("2 / 2").assertIsDisplayed()
    }

    @Test
    fun tapPreviousNavigatesToFirstVideo() {
        setMediaViewerContent(initialIndex = 1)

        composeTestRule.awaitText("2 / 2")
        composeTestRule.onNodeWithContentDescription("Previous").performClick()

        composeTestRule.awaitText("1 / 2")
        composeTestRule.onNodeWithText("1 / 2").assertIsDisplayed()
    }

    @Test
    fun speedButtonDisplayedWithDefault() {
        setVideoPlayerContent(isActive = false)

        composeTestRule.onNodeWithContentDescription("Playback speed").assertIsDisplayed()
        composeTestRule.onNodeWithText("1X").assertIsDisplayed()
    }

    @Test
    fun speedMenuOpensOnTap() {
        openSpeedMenu()

        composeTestRule.onNodeWithText("0.25X").assertIsDisplayed()
        composeTestRule.onNodeWithText("2X").assertIsDisplayed()
    }

    @Test
    fun selectingSpeedUpdatesLabel() {
        openSpeedMenu()
        composeTestRule.onNodeWithText("1.5X").performClick()

        composeTestRule.onNodeWithText("1.5X").assertIsDisplayed()
        composeTestRule.onNodeWithText("0.25X").assertDoesNotExist()
    }

    @Test
    fun speedMenuContainsAllOptions() {
        openSpeedMenu()

        listOf("0.25X", "0.5X", "0.75X", "1.25X", "1.5X", "1.75X", "2X").forEach {
            composeTestRule.onNodeWithText(it).assertExists()
        }
        composeTestRule.onAllNodes(hasText("1X")).assertCountEquals(2)
    }

    @Test
    fun speedMenuReopensWithSelectedSpeed() {
        openSpeedMenu()
        composeTestRule.onNodeWithText("0.75X").performClick()
        composeTestRule.onNodeWithText("0.75X").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Playback speed").performClick()
        composeTestRule.onNodeWithText("0.25X").assertExists()
    }

    @Test
    fun speedSelectionMinValue() {
        openSpeedMenu()
        composeTestRule.onNodeWithText("0.25X").performClick()

        composeTestRule.onNodeWithText("0.25X").assertIsDisplayed()
    }

    @Test
    fun speedSelectionMaxValue() {
        openSpeedMenu()
        composeTestRule.onNodeWithText("2X").performClick()

        composeTestRule.onNodeWithText("2X").assertIsDisplayed()
    }

    @Test
    fun allControlsDisplayedInPager() {
        setMediaViewerContent(initialIndex = 0)

        composeTestRule.awaitNode("Pause")
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Skip back", substring = true))
            .assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Skip forward", substring = true))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Previous").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Next").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Playback speed").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Delete").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    }

    @Test
    fun deleteButtonDisplayedInToolbar() {
        setMediaViewerContent(initialIndex = 0)

        composeTestRule.onNodeWithContentDescription("Delete").assertIsDisplayed()
    }

    @Test
    fun tapVideoHidesControlsAndToolbar() {
        setMediaViewerContent(initialIndex = 0)

        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Previous").assertIsDisplayed()

        composeTestRule.onRoot().performTouchInput { click(center) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Close").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Previous").assertDoesNotExist()
    }

    @Test
    fun tapVideoTogglesControlsBackOn() {
        setMediaViewerContent(initialIndex = 0)

        composeTestRule.onRoot().performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Close").assertDoesNotExist()

        composeTestRule.onRoot().performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    }

    @Test
    fun deleteButtonShowsConfirmationDialog() {
        setMediaViewerContent()

        openDeleteDialog()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Delete file?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun deleteCancelDismissesDialog() {
        var deletedFile: FileItem? = null
        setMediaViewerContent(onDelete = { deletedFile = it })

        openDeleteDialog()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Delete file?").assertDoesNotExist()
        assertNull(deletedFile)
    }

    @Test
    fun deleteConfirmCallsOnDeleteWithCurrentFile() {
        var deletedFile: FileItem? = null
        setMediaViewerContent(onDelete = { deletedFile = it })

        openDeleteDialog()
        confirmDelete()

        assertEquals(videoFileItems[0], deletedFile)
    }

    @Test
    fun deleteConfirmRemovesFileFromPager() {
        var files by mutableStateOf(videoFileItems)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalPlayerFactory provides playerFactory) {
                MediaViewerOverlay(
                    viewableFiles = files,
                    initialIndex = 0,
                    onDismiss = {},
                    onDelete = { fileItem -> files = files.filterNot { it.path == fileItem.path } },
                )
            }
        }

        composeTestRule.awaitText("1 / 2")
        openDeleteDialog()
        composeTestRule.onNodeWithText("Delete file?").assertIsDisplayed()
        confirmDelete()

        composeTestRule.awaitText("1 / 1")
        composeTestRule.onNodeWithText("1 / 1").assertIsDisplayed()
    }
}
