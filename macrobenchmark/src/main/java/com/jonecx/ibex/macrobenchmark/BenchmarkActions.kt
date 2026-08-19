package com.jonecx.ibex.macrobenchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

fun MacrobenchmarkScope.grantStoragePermission() {
    device.executeShellCommand(
        "appops set $TARGET_PACKAGE MANAGE_EXTERNAL_STORAGE allow",
    )
}

fun MacrobenchmarkScope.switchToGridView() {
    val settingsButton = device.wait(
        Until.findObject(By.desc("Settings")),
        5_000L,
    )
    requireNotNull(settingsButton) { "Settings button not found on HomeScreen" }
    settingsButton.click()
    device.waitForIdle()

    val gridOption = device.wait(Until.findObject(By.text("Grid")), 5_000L)
    requireNotNull(gridOption) { "Grid option not found in Settings" }
    gridOption.click()
    device.waitForIdle()

    device.pressBack()
    device.waitForIdle()
}

// Images/Videos open as an album list (folders holding media), so the scrollable grid lives one level in.
// Opens the source tile, then steps into the seeded [album] (from ci_seed_media.sh).
fun MacrobenchmarkScope.openMediaAlbum(tileName: String, album: String) {
    val tile = device.wait(Until.findObject(By.text(tileName)), 5_000L)
    requireNotNull(tile) { "Tile '$tileName' not found on HomeScreen" }
    tile.click()
    device.waitForIdle()

    val albumTile = device.wait(Until.findObject(By.text(album)), 5_000L)
    requireNotNull(albumTile) {
        "Album '$album' not found under '$tileName' — did ci_seed_media.sh seed it and is storage granted?"
    }
    albumTile.click()
}

fun MacrobenchmarkScope.scrollContent(tileName: String) {
    val list = device.wait(Until.findObject(By.scrollable(true)), 10_000L)
    requireNotNull(list) {
        "No scrollable content in '$tileName' — is MANAGE_EXTERNAL_STORAGE granted?"
    }
    list.setGestureMargin(device.displayWidth / 5)
    list.fling(Direction.DOWN)
    device.waitForIdle()
    list.fling(Direction.UP)
    device.waitForIdle()
}
