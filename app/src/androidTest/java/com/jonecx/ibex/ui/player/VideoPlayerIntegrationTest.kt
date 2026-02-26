package com.jonecx.ibex.ui.player

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.inject.Inject

private const val TEST_VIDEO_ASSET = "earth_MP4_480_1_5MG.mp4"
private const val PLAYBACK_TIMEOUT_MS = 5_000L

@HiltAndroidTest
class VideoPlayerIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Inject
    lateinit var playerFactory: PlayerFactory

    private lateinit var videoFileItem: FileItem

    @Before
    fun setup() {
        hiltRule.inject()

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val videoFile = File(instrumentation.targetContext.cacheDir, TEST_VIDEO_ASSET)
        if (!videoFile.exists()) {
            instrumentation.context.assets.open(TEST_VIDEO_ASSET).use { input ->
                videoFile.outputStream().use { output -> input.copyTo(output) }
            }
        }

        videoFileItem = FileItem(
            name = TEST_VIDEO_ASSET,
            path = videoFile.absolutePath,
            uri = Uri.fromFile(videoFile),
            size = videoFile.length(),
            lastModified = videoFile.lastModified(),
            isDirectory = false,
            fileType = FileType.VIDEO,
        )
    }

    private fun setVideoPlayerContent(isActive: Boolean) {
        composeTestRule.setContent {
            VideoPlayer(
                fileItem = videoFileItem,
                isActive = isActive,
                playerFactory = playerFactory,
            )
        }
    }

    private fun ComposeContentTestRule.awaitControlDescription(description: String) {
        waitUntil(timeoutMillis = PLAYBACK_TIMEOUT_MS) {
            onAllNodes(hasContentDescription(description))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun controlsAreDisplayed() {
        setVideoPlayerContent(isActive = false)

        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Seek back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Seek forward").assertIsDisplayed()
    }

    @Test
    fun videoPlaysWhenActive() {
        setVideoPlayerContent(isActive = true)

        composeTestRule.awaitControlDescription("Pause")
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun tapPlayStartsPlayback() {
        setVideoPlayerContent(isActive = false)

        composeTestRule.onNodeWithContentDescription("Play").performClick()

        composeTestRule.awaitControlDescription("Pause")
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun tapPausePausesPlayback() {
        setVideoPlayerContent(isActive = true)

        composeTestRule.awaitControlDescription("Pause")
        composeTestRule.onNodeWithContentDescription("Pause").performClick()

        composeTestRule.awaitControlDescription("Play")
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }
}
