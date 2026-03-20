package com.jonecx.ibex.data.repository

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FileOperationsIntegrationTest {

    companion object {
        private const val HTML_ASSET = "html-fil-samle.html"
        private const val PDF_ASSET = "pdf-file-sample_150kB.pdf"
        private const val RTF_ASSET = "rtf-file-sample_100kB.rtf"
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var moveManager: FileSystemMoveManager
    private lateinit var testDir: File
    private lateinit var sourceDir: File
    private lateinit var destinationDir: File

    @Before
    fun setup() {
        moveManager = FileSystemMoveManager(testDispatcher)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDir = File(context.cacheDir, "file_ops_test_${System.nanoTime()}")
        testDir.mkdirs()

        sourceDir = File(testDir, "source").apply { mkdirs() }
        destinationDir = File(testDir, "destination").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    private fun copyAssetToSource(assetName: String): File {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val destFile = File(sourceDir, assetName)
        instrumentation.context.assets.open(assetName).use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        return destFile
    }

    private fun fileItemOf(file: File): FileItem = FileItem(
        name = file.name,
        path = file.absolutePath,
        uri = Uri.fromFile(file),
        size = file.length(),
        lastModified = file.lastModified(),
        isDirectory = file.isDirectory,
        fileType = if (file.isDirectory) FileType.DIRECTORY else FileType.UNKNOWN,
    )

    private fun assertFileMoved(source: File, destDir: File, expectedSize: Long) {
        assertFalse(source.exists())
        val moved = File(destDir, source.name)
        assertTrue(moved.exists())
        assertEquals(expectedSize, moved.length())
    }

    private fun assertFileCopied(source: File, destDir: File, expectedSize: Long) {
        assertTrue(source.exists())
        assertEquals(expectedSize, source.length())
        val copied = File(destDir, source.name)
        assertTrue(copied.exists())
        assertEquals(expectedSize, copied.length())
    }

    private fun assertFileRenamed(source: File, renamed: File, expectedSize: Long) {
        assertFalse(source.exists())
        assertTrue(renamed.exists())
        assertEquals(expectedSize, renamed.length())
    }

    @Test
    fun moveHtmlFileToExistingFolder() = runTest {
        val file = copyAssetToSource(HTML_ASSET)
        val originalSize = file.length()

        assertTrue(moveManager.moveFile(fileItemOf(file), destinationDir.absolutePath))
        assertFileMoved(file, destinationDir, originalSize)
    }

    @Test
    fun movePdfFileToExistingFolder() = runTest {
        val file = copyAssetToSource(PDF_ASSET)
        val originalSize = file.length()

        assertTrue(moveManager.moveFile(fileItemOf(file), destinationDir.absolutePath))
        assertFileMoved(file, destinationDir, originalSize)
    }

    @Test
    fun moveRtfFileToExistingFolder() = runTest {
        val file = copyAssetToSource(RTF_ASSET)
        val originalSize = file.length()

        assertTrue(moveManager.moveFile(fileItemOf(file), destinationDir.absolutePath))
        assertFileMoved(file, destinationDir, originalSize)
    }

    @Test
    fun moveFileToNewlyCreatedFolder() = runTest {
        val file = copyAssetToSource(PDF_ASSET)
        val originalSize = file.length()
        val newFolder = "Documents"

        val folderCreated = moveManager.createFolder(destinationDir.absolutePath, newFolder)
        assertTrue(folderCreated)

        val targetDir = File(destinationDir, newFolder)
        val result = moveManager.moveFile(fileItemOf(file), targetDir.absolutePath)

        assertTrue(result)
        assertFileMoved(file, targetDir, originalSize)
    }

    @Test
    fun moveFileFailsWhenSourceMissing() = runTest {
        val ghost = File(sourceDir, "ghost.pdf")

        val result = moveManager.moveFile(fileItemOf(ghost), destinationDir.absolutePath)

        assertFalse(result)
    }

    @Test
    fun copyHtmlFileToExistingFolder() = runTest {
        val file = copyAssetToSource(HTML_ASSET)
        val originalSize = file.length()

        assertTrue(moveManager.copyFile(fileItemOf(file), destinationDir.absolutePath))
        assertFileCopied(file, destinationDir, originalSize)
    }

    @Test
    fun copyPdfFileToExistingFolder() = runTest {
        val file = copyAssetToSource(PDF_ASSET)
        val originalSize = file.length()

        assertTrue(moveManager.copyFile(fileItemOf(file), destinationDir.absolutePath))
        assertFileCopied(file, destinationDir, originalSize)
    }

    @Test
    fun copyRtfFileToExistingFolder() = runTest {
        val file = copyAssetToSource(RTF_ASSET)
        val originalSize = file.length()

        assertTrue(moveManager.copyFile(fileItemOf(file), destinationDir.absolutePath))
        assertFileCopied(file, destinationDir, originalSize)
    }

    @Test
    fun copyFileToNewlyCreatedFolder() = runTest {
        val file = copyAssetToSource(HTML_ASSET)
        val originalSize = file.length()
        val newFolder = "WebPages"

        val folderCreated = moveManager.createFolder(destinationDir.absolutePath, newFolder)
        assertTrue(folderCreated)

        val targetDir = File(destinationDir, newFolder)
        val result = moveManager.copyFile(fileItemOf(file), targetDir.absolutePath)

        assertTrue(result)
        assertFileCopied(file, targetDir, originalSize)
    }

    @Test
    fun copyFileFailsWhenSourceMissing() = runTest {
        val ghost = File(sourceDir, "ghost.pdf")

        val result = moveManager.copyFile(fileItemOf(ghost), destinationDir.absolutePath)

        assertFalse(result)
    }

    @Test
    fun renameHtmlFile() = runTest {
        val file = copyAssetToSource(HTML_ASSET)
        val originalSize = file.length()

        assertTrue(moveManager.renameFile(fileItemOf(file), "renamed-page.html"))
        assertFileRenamed(file, File(sourceDir, "renamed-page.html"), originalSize)
    }

    @Test
    fun renamePdfFile() = runTest {
        val file = copyAssetToSource(PDF_ASSET)
        val originalSize = file.length()

        assertTrue(moveManager.renameFile(fileItemOf(file), "my-document.pdf"))
        assertFileRenamed(file, File(sourceDir, "my-document.pdf"), originalSize)
    }

    @Test
    fun renameRtfFile() = runTest {
        val file = copyAssetToSource(RTF_ASSET)
        val originalSize = file.length()

        assertTrue(moveManager.renameFile(fileItemOf(file), "notes.rtf"))
        assertFileRenamed(file, File(sourceDir, "notes.rtf"), originalSize)
    }

    @Test
    fun renameFileFailsWhenSourceMissing() = runTest {
        val ghost = File(sourceDir, "ghost.pdf")

        val result = moveManager.renameFile(fileItemOf(ghost), "new-name.pdf")

        assertFalse(result)
    }

    @Test
    fun createFolderCreatesNewDirectory() = runTest {
        val result = moveManager.createFolder(sourceDir.absolutePath, "NewFolder")

        assertTrue(result)
        val created = File(sourceDir, "NewFolder")
        assertTrue(created.exists() && created.isDirectory)
    }

    @Test
    fun createFolderFailsWhenAlreadyExists() = runTest {
        File(sourceDir, "Existing").mkdirs()

        val result = moveManager.createFolder(sourceDir.absolutePath, "Existing")

        assertFalse(result)
    }

    @Test
    fun createFolderFailsWhenParentMissing() = runTest {
        val result = moveManager.createFolder(
            File(testDir, "nonexistent_parent").absolutePath,
            "child",
        )

        assertFalse(result)
    }

    @Test
    fun createFolderThenMoveMultipleFilesInto() = runTest {
        val htmlFile = copyAssetToSource(HTML_ASSET)
        val pdfFile = copyAssetToSource(PDF_ASSET)
        val rtfFile = copyAssetToSource(RTF_ASSET)
        val htmlSize = htmlFile.length()
        val pdfSize = pdfFile.length()
        val rtfSize = rtfFile.length()

        val folderCreated = moveManager.createFolder(destinationDir.absolutePath, "AllDocs")
        assertTrue(folderCreated)
        val targetDir = File(destinationDir, "AllDocs")

        assertTrue(moveManager.moveFile(fileItemOf(htmlFile), targetDir.absolutePath))
        assertTrue(moveManager.moveFile(fileItemOf(pdfFile), targetDir.absolutePath))
        assertTrue(moveManager.moveFile(fileItemOf(rtfFile), targetDir.absolutePath))

        assertFalse(htmlFile.exists())
        assertFalse(pdfFile.exists())
        assertFalse(rtfFile.exists())

        assertEquals(htmlSize, File(targetDir, HTML_ASSET).length())
        assertEquals(pdfSize, File(targetDir, PDF_ASSET).length())
        assertEquals(rtfSize, File(targetDir, RTF_ASSET).length())
    }

    @Test
    fun deleteFileRemovesFile() = runTest {
        val file = copyAssetToSource(HTML_ASSET)
        assertTrue(file.exists())

        val result = moveManager.deleteFile(fileItemOf(file))

        assertTrue(result)
        assertFalse(file.exists())
    }

    @Test
    fun deleteDirectoryRemovesRecursively() = runTest {
        val subDir = File(sourceDir, "subdir").apply { mkdirs() }
        val file = copyAssetToSource(HTML_ASSET)
        val nestedFile = File(subDir, HTML_ASSET)
        file.copyTo(nestedFile)

        val result = moveManager.deleteFile(fileItemOf(subDir))

        assertTrue(result)
        assertFalse(subDir.exists())
        assertFalse(nestedFile.exists())
    }

    @Test
    fun deleteFileFailsWhenSourceMissing() = runTest {
        val missing = File(sourceDir, "nonexistent.txt")
        val result = moveManager.deleteFile(fileItemOf(missing))
        assertFalse(result)
    }
}
