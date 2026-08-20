package com.jonecx.ibex.data.repository

import com.jonecx.ibex.fixtures.testFileItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files

// Robolectric supplies android.net.Uri for the testFileItem fixture; copyFile itself only reads path.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FileSystemMoveManagerCopyTest {

    private val handler = FileSystemMoveManager(UnconfinedTestDispatcher())
    private val tempRoots = mutableListOf<File>()

    @After
    fun tearDown() {
        tempRoots.forEach { it.deleteRecursively() }
    }

    @Test
    fun `copyFile_singleFile_copiesAndReturnsTrue`() = runTest {
        val srcDir = newTempDir()
        val source = File(srcDir, "payload.bin").apply { writeText("payload") }
        val destDir = newTempDir()

        val result = handler.copyFile(itemFor(source), destDir.path)

        assertTrue(result)
        assertEquals("payload", File(destDir, source.name).readText())
    }

    @Test
    fun `copyFile_directory_copiesTreeAndReturnsTrue`() = runTest {
        val srcDir = newTempDir()
        File(srcDir, "child.txt").writeText("data")
        val destParent = newTempDir()

        val result = handler.copyFile(itemFor(srcDir), destParent.path)

        assertTrue(result)
        assertEquals("data", File(destParent, "${srcDir.name}/child.txt").readText())
    }

    @Test
    fun `copyFile_existingDestination_returnsFalse`() = runTest {
        val srcDir = newTempDir()
        val source = File(srcDir, "payload.bin").apply { writeText("payload") }
        val destDir = newTempDir()
        File(destDir, source.name).writeText("existing")

        val result = handler.copyFile(itemFor(source), destDir.path)

        assertFalse(result)
    }

    @Test
    fun `copyFile_directoryWithConflict_returnsFalse`() = runTest {
        val srcDir = newTempDir()
        File(srcDir, "child.txt").writeText("data")
        val destParent = newTempDir()
        val destDir = File(destParent, srcDir.name).apply { mkdirs() }
        File(destDir, "child.txt").writeText("existing")

        val result = handler.copyFile(itemFor(srcDir), destParent.path)

        assertFalse(result)
    }

    @Test
    fun `copyFile_missingSource_returnsFalse`() = runTest {
        val destDir = newTempDir()

        val result = handler.copyFile(testFileItem("gone.bin", path = "/no/such/source.bin"), destDir.path)

        assertFalse(result)
    }

    private fun newTempDir(): File = Files.createTempDirectory("fsmm").toFile().also { tempRoots.add(it) }

    private fun itemFor(source: File) = testFileItem(source.name, path = source.path)
}
