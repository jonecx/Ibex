package com.jonecx.ibex.macrobenchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
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
            navigateAndScroll("Images")

            // Scroll Videos — captures video thumbnail loading
            navigateAndScroll("Videos")
        }
    }

    private fun MacrobenchmarkScope.navigateAndScroll(tileName: String) {
        val tile = device.wait(Until.findObject(By.text(tileName)), 5_000L)
        requireNotNull(tile) { "Tile '$tileName' not found on HomeScreen" }
        tile.click()

        scrollContent(tileName)

        device.pressBack()
        device.waitForIdle()
    }
}
