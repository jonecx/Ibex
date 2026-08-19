package com.jonecx.ibex.macrobenchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateProfile() {
        rule.collect(
            packageName = TARGET_PACKAGE,
            includeInStartupProfile = true,
        ) {
            // Grant storage permission after the library reinstalls the app
            grantStoragePermission()

            // Cold start — captures startup code paths
            pressHome()
            startActivityAndWait()

            // Switch to grid view — captures settings + preference code paths
            switchToGridView()

            // Scroll Images — captures file explorer, Coil thumbnails, LazyVerticalGrid
            navigateAndScroll("Images", album = IMAGES_ALBUM)

            // Scroll Videos — captures video thumbnail loading
            navigateAndScroll("Videos", album = VIDEOS_ALBUM)
        }
    }

    // Steps into the seeded album before scrolling, then back out twice: album list, then home screen.
    private fun MacrobenchmarkScope.navigateAndScroll(tileName: String, album: String) {
        openMediaAlbum(tileName, album)

        scrollContent(tileName)

        device.pressBack()
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }
}
