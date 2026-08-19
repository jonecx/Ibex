package com.jonecx.ibex.macrobenchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until

fun MacrobenchmarkScope.grantStoragePermission() {
    device.executeShellCommand(
        "appops set $TARGET_PACKAGE MANAGE_EXTERNAL_STORAGE allow",
    )
}

fun MacrobenchmarkScope.switchToGridView() {
    clickStable(By.desc("Settings"), 5_000L) { "Settings button not found on HomeScreen" }
    clickStable(By.text("Grid"), 5_000L) { "Grid option not found in Settings" }
    device.pressBack()
    device.waitForIdle()
}

// Images/Videos open as an album list (folders holding media), so the scrollable grid lives one level in.
// Opens the source tile, then steps into the seeded [album] (from ci_seed_media.sh).
fun MacrobenchmarkScope.openMediaAlbum(tileName: String, album: String) {
    clickStable(By.text(tileName), 5_000L) { "Tile '$tileName' not found on HomeScreen" }
    clickStable(By.text(album), 5_000L) {
        "Album '$album' not found under '$tileName' — did ci_seed_media.sh seed it and is storage granted?"
    }
}

fun MacrobenchmarkScope.scrollContent(tileName: String) {
    // Confirm the grid loaded, then scroll by raw display coordinates instead of driving the
    // UiObject2. The images grid keeps recomposing as thumbnails decode, so any live-node access
    // (fling/getVisibleBounds) can go stale mid-gesture (StaleObjectException). Coordinates never
    // touch the accessibility node, so the gesture cannot go stale.
    val list = device.wait(Until.findObject(By.scrollable(true)), 10_000L)
    requireNotNull(list) {
        "No scrollable content in '$tileName' — is MANAGE_EXTERNAL_STORAGE granted?"
    }
    val x = device.displayWidth / 2
    // Stay well inside the screen so the gesture is not read as a system edge swipe.
    val top = device.displayHeight / 4
    val bottom = device.displayHeight * 3 / 4
    device.swipe(x, bottom, x, top, SWIPE_STEPS) // scroll down
    device.waitForIdle()
    device.swipe(x, top, x, bottom, SWIPE_STEPS) // scroll up
    device.waitForIdle()
}

// Clicks the first node matching [selector], re-finding on staleness. Navigation screens stream in
// folder-cover thumbnails and recompose, replacing the node behind a UiObject2, so a plain
// find-then-click races and throws StaleObjectException; re-find and retry until the deadline.
private fun MacrobenchmarkScope.clickStable(
    selector: BySelector,
    timeoutMs: Long,
    missing: () -> String,
) {
    val deadline = System.currentTimeMillis() + CLICK_RETRY_TIMEOUT_MS
    var lastStale: StaleObjectException? = null
    do {
        val target = device.wait(Until.findObject(selector), timeoutMs)
        requireNotNull(target, missing)
        try {
            target.click()
            device.waitForIdle()
            return
        } catch (e: StaleObjectException) {
            lastStale = e
            device.waitForIdle()
        }
    } while (System.currentTimeMillis() < deadline)
    throw requireNotNull(lastStale)
}

// Fewer steps = faster swipe; 10 gives a fling-like scroll while staying deterministic.
private const val SWIPE_STEPS = 10

private const val CLICK_RETRY_TIMEOUT_MS = 10_000L
