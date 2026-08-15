package com.jonecx.ibex.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

// Guards the brand palette against contrast regressions: the theme is deliberately monochrome plus one red,
// so any future colour edit must still clear WCAG AA for the pairings the UI actually renders.
class ThemeContrastTest {

    private fun channel(component: Float): Float =
        if (component <= 0.03928f) component / 12.92f else ((component + 0.055f) / 1.055f).pow(2.4f)

    private fun luminance(color: Color): Float =
        0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)

    private fun contrast(foreground: Color, background: Color): Float {
        val lighter = luminance(foreground) + 0.05f
        val darker = luminance(background) + 0.05f
        return if (lighter > darker) lighter / darker else darker / lighter
    }

    private fun assertContrast(foreground: Color, background: Color, min: Float, label: String) {
        val ratio = contrast(foreground, background)
        assertTrue("$label was ${"%.2f".format(ratio)}:1, needed $min:1", ratio >= min)
    }

    private val light = LightColorScheme
    private val dark = DarkColorScheme

    @Test
    fun neutralSelectionLabel_light_meetsAaText() {
        assertContrast(light.onSecondaryContainer, light.secondaryContainer, AA_TEXT, "light neutral label")
    }

    @Test
    fun neutralSelectionLabel_dark_meetsAaText() {
        assertContrast(dark.onSecondaryContainer, dark.secondaryContainer, AA_TEXT, "dark neutral label")
    }

    @Test
    fun destructiveText_lightSelectionBar_meetsAaText() {
        assertContrast(light.error, light.secondaryContainer, AA_TEXT, "light Delete label")
    }

    @Test
    fun destructiveText_darkSelectionBar_meetsAaText() {
        assertContrast(dark.error, dark.secondaryContainer, AA_TEXT, "dark Delete label")
    }

    @Test
    fun destructiveText_onWhite_meetsAaText() {
        assertContrast(light.error, Color.White, AA_TEXT, "destructive red on white")
    }

    @Test
    fun destructiveText_onLightPage_meetsAaText() {
        assertContrast(light.error, light.surface, AA_TEXT, "destructive red on light page")
    }

    @Test
    fun destructiveText_onDarkPage_meetsAaText() {
        assertContrast(dark.error, dark.surface, AA_TEXT, "destructive red on dark page")
    }

    @Test
    fun selectionCheckGlyph_light_meetsGraphicContrast() {
        assertContrast(light.onPrimary, light.primary, AA_GRAPHIC, "light selection tick")
    }

    @Test
    fun selectionCheckGlyph_dark_meetsGraphicContrast() {
        assertContrast(dark.onPrimary, dark.primary, AA_GRAPHIC, "dark selection tick")
    }

    @Test
    fun fabIcon_light_meetsGraphicContrast() {
        assertContrast(light.onPrimaryContainer, light.primaryContainer, AA_GRAPHIC, "light FAB icon")
    }

    @Test
    fun fabIcon_dark_meetsGraphicContrast() {
        assertContrast(dark.onPrimaryContainer, dark.primaryContainer, AA_GRAPHIC, "dark FAB icon")
    }

    private companion object {
        const val AA_TEXT = 4.5f
        const val AA_GRAPHIC = 3.0f
    }
}
