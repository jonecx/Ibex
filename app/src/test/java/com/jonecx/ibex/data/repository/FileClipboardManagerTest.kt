package com.jonecx.ibex.data.repository

import com.jonecx.ibex.fixtures.FakeFileMoveManager
import com.jonecx.ibex.fixtures.testFileItem
import kotlinx.coroutines.test.runTest
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

    private lateinit var fakeMoveManager: FakeFileMoveManager
    private lateinit var clipboardManager: FileClipboardManager

    @Before
    fun setup() {
        fakeMoveManager = FakeFileMoveManager()
        clipboardManager = DefaultFileClipboardManager(fakeMoveManager)
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

    @Test
    fun `paste with MOVE calls moveFile for each file`() = runTest {
        val file1 = testFileItem("a.txt")
        val file2 = testFileItem("b.txt")
        clipboardManager.setClipboard(listOf(file1, file2), ClipboardOperation.MOVE)

        val result = clipboardManager.paste("/dest")

        assertTrue(result)
        assertEquals(2, fakeMoveManager.movedFiles.size)
        assertEquals("/dest", fakeMoveManager.movedFiles[0].second)
        assertTrue(fakeMoveManager.copiedFiles.isEmpty())
        assertFalse(clipboardManager.state.value.hasContent)
    }

    @Test
    fun `paste with COPY calls copyFile for each file`() = runTest {
        val file1 = testFileItem("a.txt")
        clipboardManager.setClipboard(listOf(file1), ClipboardOperation.COPY)

        val result = clipboardManager.paste("/dest")

        assertTrue(result)
        assertEquals(1, fakeMoveManager.copiedFiles.size)
        assertEquals("/dest", fakeMoveManager.copiedFiles[0].second)
        assertTrue(fakeMoveManager.movedFiles.isEmpty())
        assertFalse(clipboardManager.state.value.hasContent)
    }

    @Test
    fun `paste with empty clipboard returns false`() = runTest {
        val result = clipboardManager.paste("/dest")

        assertFalse(result)
        assertTrue(fakeMoveManager.movedFiles.isEmpty())
        assertTrue(fakeMoveManager.copiedFiles.isEmpty())
    }

    @Test
    fun `paste clears clipboard after completion`() = runTest {
        clipboardManager.setClipboard(listOf(testFileItem("a.txt")), ClipboardOperation.MOVE)
        clipboardManager.paste("/dest")

        assertFalse(clipboardManager.state.value.hasContent)
    }
}
