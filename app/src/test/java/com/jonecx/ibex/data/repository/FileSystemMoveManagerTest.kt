package com.jonecx.ibex.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FileSystemMoveManagerTest {

    private val handler = FileSystemMoveManager(UnconfinedTestDispatcher())
    private lateinit var file: File

    @Before
    fun setup() {
        file = File.createTempFile("fsmm", ".bin")
    }

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun `sizeOf_existingFile_returnsLength`() = runTest {
        file.writeBytes(ByteArray(10))

        assertEquals(10L, handler.sizeOf(file.path))
    }

    @Test
    fun `sizeOf_missingFile_returnsMinusOne`() = runTest {
        assertEquals(-1L, handler.sizeOf("/no/such/path.bin"))
    }

    @Test
    fun `openInputStream_withOffset_startsAtOffset`() = runTest {
        file.writeText("0123456789")

        val read = handler.openInputStream(file.path, 5).use { String(it.readBytes()) }

        assertEquals("56789", read)
    }

    @Test
    fun `openOutputStream_withOffset_appendsFromOffset`() = runTest {
        file.writeText("01234")

        handler.openOutputStream(file.path, 5).use { it.write("56789".toByteArray()) }

        assertEquals("0123456789", file.readText())
    }

    @Test
    fun `openOutputStream_zeroOffset_truncates`() = runTest {
        file.writeText("stale contents")

        handler.openOutputStream(file.path, 0).use { it.write("new".toByteArray()) }

        assertEquals("new", file.readText())
    }
}
