package com.jonecx.ibex.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private var gridEnabled = false

    // --- Images (grid view) ---

    @Test
    fun scrollImagesGridCompilationNone() =
        scrollSource("Images", useGrid = true, CompilationMode.None())

    @Test
    fun scrollImagesGridBaselineProfile() =
        scrollSource("Images", useGrid = true, CompilationMode.Partial())

    // --- Videos (grid view) ---

    @Test
    fun scrollVideosGridCompilationNone() =
        scrollSource("Videos", useGrid = true, CompilationMode.None())

    @Test
    fun scrollVideosGridBaselineProfile() =
        scrollSource("Videos", useGrid = true, CompilationMode.Partial())

    private fun scrollSource(
        tileName: String,
        useGrid: Boolean,
        compilationMode: CompilationMode,
    ) {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = DEFAULT_ITERATIONS,
            startupMode = StartupMode.WARM,
            compilationMode = compilationMode,
            setupBlock = {
                grantStoragePermission()
                pressHome()
                startActivityAndWait()

                if (useGrid && !gridEnabled) {
                    switchToGridView()
                    gridEnabled = true
                }

                val tile = device.wait(Until.findObject(By.text(tileName)), 5_000L)
                requireNotNull(tile) { "Tile '$tileName' not found on HomeScreen" }
                tile.click()

                device.wait(Until.findObject(By.scrollable(true)), 10_000L)
            },
        ) {
            scrollContent(tileName)
        }
    }
}
