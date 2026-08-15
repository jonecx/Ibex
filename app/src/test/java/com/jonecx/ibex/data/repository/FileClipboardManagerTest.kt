package com.jonecx.ibex.data.repository

import com.jonecx.ibex.fixtures.testFileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileClipboardManagerTest {

    private lateinit var clipboardManager: FileClipboardManager

    @Before
    fun setup() {
        clipboardManager = DefaultFileClipboardManager()
    }

    @Test
    fun `initial state is empty`() {
        val state = clipboardManager.state.value
        assertFalse(state.hasContent)
        assertTrue(state.files.isEmpty())
        assertNull(state.operation)
    }

    @Test
    fun `setClipboard stores files and operation`() {
        val files = listOf(testFileItem("a.txt"), testFileItem("b.txt"))
        clipboardManager.setClipboard(files, ClipboardOperation.MOVE)

        val state = clipboardManager.state.value
        assertTrue(state.hasContent)
        assertEquals(2, state.files.size)
        assertEquals(ClipboardOperation.MOVE, state.operation)
    }

    @Test
    fun `clear resets state`() {
        clipboardManager.setClipboard(listOf(testFileItem("a.txt")), ClipboardOperation.COPY)
        assertTrue(clipboardManager.state.value.hasContent)

        clipboardManager.clear()

        assertFalse(clipboardManager.state.value.hasContent)
    }
}
