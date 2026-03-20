package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.fixtures.testFileItem
import com.jonecx.ibex.fixtures.testRemoteFileItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CompositeFileMoveManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var localHandler: FakeProtocolFileHandler
    private lateinit var smbHandler: FakeProtocolFileHandler
    private lateinit var composite: CompositeFileMoveManager

    @Before
    fun setup() {
        localHandler = FakeProtocolFileHandler(prefix = null)
        smbHandler = FakeProtocolFileHandler(prefix = "smb://")
        composite = CompositeFileMoveManager(setOf(localHandler, smbHandler), testDispatcher)
    }

    @Test
    fun `moveFile routes to local handler for local paths`() = runTest {
        val file = testFileItem("a.txt")
        composite.moveFile(file, "/storage/dest")
        assertEquals(1, localHandler.moveCount)
        assertEquals(0, smbHandler.moveCount)
    }

    @Test
    fun `moveFile routes to smb handler for smb paths`() = runTest {
        val file = testRemoteFileItem("a.txt")
        composite.moveFile(file, "smb://host/share/dest")
        assertEquals(0, localHandler.moveCount)
        assertEquals(1, smbHandler.moveCount)
    }

    @Test
    fun `copyFile routes to local handler for local paths`() = runTest {
        val file = testFileItem("a.txt")
        composite.copyFile(file, "/storage/dest")
        assertEquals(1, localHandler.copyCount)
    }

    @Test
    fun `copyFile routes to smb handler for smb paths`() = runTest {
        val file = testRemoteFileItem("a.txt")
        composite.copyFile(file, "smb://host/share/dest")
        assertEquals(1, smbHandler.copyCount)
    }

    @Test
    fun `renameFile routes to local handler for local paths`() = runTest {
        val file = testFileItem("a.txt")
        composite.renameFile(file, "b.txt")
        assertEquals(1, localHandler.renameCount)
    }

    @Test
    fun `renameFile routes to smb handler for smb paths`() = runTest {
        val file = testRemoteFileItem("a.txt")
        composite.renameFile(file, "b.txt")
        assertEquals(1, smbHandler.renameCount)
    }

    @Test
    fun `createFolder routes to local handler for local paths`() = runTest {
        composite.createFolder("/storage/dir", "new")
        assertEquals(1, localHandler.createFolderCount)
    }

    @Test
    fun `createFolder routes to smb handler for smb paths`() = runTest {
        composite.createFolder("smb://host/share/dir", "new")
        assertEquals(1, smbHandler.createFolderCount)
    }

    @Test
    fun `deleteFile routes to local handler for local paths`() = runTest {
        val file = testFileItem("a.txt")
        composite.deleteFile(file)
        assertEquals(1, localHandler.deleteCount)
    }

    @Test
    fun `deleteFile routes to smb handler for smb paths`() = runTest {
        val file = testRemoteFileItem("a.txt")
        composite.deleteFile(file)
        assertEquals(1, smbHandler.deleteCount)
    }

    @Test
    fun `cross protocol copy streams from source to destination`() = runTest {
        val file = testFileItem("a.txt")
        val content = "hello world".toByteArray()
        localHandler.inputStreamContent = content

        composite.copyFile(file, "smb://host/share/dest")

        assertEquals(1, localHandler.inputStreamOpenCount)
        assertEquals(1, smbHandler.outputStreamOpenCount)
        assertTrue(smbHandler.outputStreamBytes.contentEquals(content))
    }

    @Test
    fun `cross protocol move streams then deletes source`() = runTest {
        val file = testRemoteFileItem("a.txt")
        val content = "remote data".toByteArray()
        smbHandler.inputStreamContent = content

        composite.moveFile(file, "/storage/dest")

        assertEquals(1, smbHandler.inputStreamOpenCount)
        assertEquals(1, localHandler.outputStreamOpenCount)
        assertTrue(localHandler.outputStreamBytes.contentEquals(content))
        assertEquals(1, smbHandler.deleteCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `moveFile throws when no handler matches path`() = runTest {
        val file = testFileItem("a.txt", path = "ftp://host/file.txt")
        composite.moveFile(file, "ftp://host/dest")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `copyFile throws when no handler matches path`() = runTest {
        val file = testFileItem("a.txt", path = "ftp://host/file.txt")
        composite.copyFile(file, "ftp://host/dest")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deleteFile throws when no handler matches path`() = runTest {
        val file = testFileItem("a.txt", path = "ftp://host/file.txt")
        composite.deleteFile(file)
    }

    @Test
    fun `moveFile returns false when handler fails`() = runTest {
        localHandler.shouldSucceed = false
        val file = testFileItem("a.txt")
        assertFalse(composite.moveFile(file, "/storage/dest"))
    }

    @Test
    fun `copyFile returns false when handler fails`() = runTest {
        smbHandler.shouldSucceed = false
        val file = testRemoteFileItem("a.txt")
        assertFalse(composite.copyFile(file, "smb://host/share/dest"))
    }

    @Test
    fun `cross protocol move does not delete source when copy fails`() = runTest {
        localHandler.shouldFailOutputStream = true
        val file = testRemoteFileItem("a.txt")
        smbHandler.inputStreamContent = "data".toByteArray()

        val result = composite.moveFile(file, "/storage/dest")

        assertFalse(result)
        assertEquals(0, smbHandler.deleteCount)
    }
}

private class FakeProtocolFileHandler(
    private val prefix: String?,
) : ProtocolFileHandler {

    var shouldSucceed = true
    var shouldFailOutputStream = false
    var moveCount = 0
        private set
    var copyCount = 0
        private set
    var renameCount = 0
        private set
    var createFolderCount = 0
        private set
    var deleteCount = 0
        private set
    var inputStreamOpenCount = 0
        private set
    var outputStreamOpenCount = 0
        private set
    var inputStreamContent = ByteArray(0)
    var outputStreamBytes = ByteArray(0)
        private set

    override fun canHandle(path: String): Boolean =
        if (prefix == null) !path.contains("://") else path.startsWith(prefix)

    override suspend fun moveFile(fileItem: FileItem, destinationDir: String): Boolean {
        moveCount++
        return shouldSucceed
    }

    override suspend fun copyFile(fileItem: FileItem, destinationDir: String): Boolean {
        copyCount++
        return shouldSucceed
    }

    override suspend fun renameFile(fileItem: FileItem, newName: String): Boolean {
        renameCount++
        return shouldSucceed
    }

    override suspend fun createFolder(parentDir: String, name: String): Boolean {
        createFolderCount++
        return shouldSucceed
    }

    override suspend fun deleteFile(fileItem: FileItem): Boolean {
        deleteCount++
        return shouldSucceed
    }

    override suspend fun openInputStream(path: String): InputStream {
        inputStreamOpenCount++
        return ByteArrayInputStream(inputStreamContent)
    }

    override suspend fun openOutputStream(path: String): OutputStream {
        if (shouldFailOutputStream) throw java.io.IOException("Simulated write failure")
        outputStreamOpenCount++
        return object : ByteArrayOutputStream() {
            override fun close() {
                super.close()
                outputStreamBytes = toByteArray()
            }
        }
    }

    override suspend fun listFiles(path: String): List<FileItem> = emptyList()
}
