package com.jonecx.ibex.data.transfer

import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.fixtures.InMemoryProtocolHandler
import com.jonecx.ibex.fixtures.MEM_SCHEME
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TransferEngineTest {

    private val handler = InMemoryProtocolHandler()
    private val engine = TransferEngine(setOf(handler), UnconfinedTestDispatcher())

    private val part = ".ibexpart"

    private fun source(name: String, bytes: ByteArray): TransferSource {
        handler.files["$MEM_SCHEME/src/$name"] = bytes
        return TransferSource("$MEM_SCHEME/src/$name", name, bytes.size.toLong(), false)
    }

    @Test
    fun `transfer_copyFile_writesFinalAndLeavesNoTemp`() = runTest {
        val data = "hello world".toByteArray()
        val src = source("a.txt", data)
        val listener = RecordingListener()

        engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.COPY, listener)

        assertArrayEquals(data, handler.files["$MEM_SCHEME/dest/a.txt"])
        assertNull(handler.files["$MEM_SCHEME/dest/a.txt$part"])
        assertEquals(data.size.toLong(), listener.bytes)
        assertEquals(1, listener.filesComplete)
    }

    @Test
    fun `transfer_resumesFromPartialTemp`() = runTest {
        val data = "0123456789".toByteArray()
        val src = source("a.txt", data)
        // Pretend a prior run wrote the first 5 bytes into the temp before dying.
        handler.files["$MEM_SCHEME/dest/a.txt$part"] = "01234".toByteArray()

        engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.COPY, RecordingListener())

        assertArrayEquals(data, handler.files["$MEM_SCHEME/dest/a.txt"])
        assertNull(handler.files["$MEM_SCHEME/dest/a.txt$part"])
    }

    @Test
    fun `transfer_skipsFileAlreadyCompleteAtDestination`() = runTest {
        val data = "already here".toByteArray()
        val src = source("a.txt", data)
        handler.files["$MEM_SCHEME/dest/a.txt"] = data.copyOf()
        val listener = RecordingListener()

        engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.COPY, listener)

        // Counted as done, but never re-read (no temp created).
        assertNull(handler.files["$MEM_SCHEME/dest/a.txt$part"])
        assertEquals(data.size.toLong(), listener.bytes)
        assertEquals(1, listener.filesComplete)
    }

    @Test
    fun `transfer_nameCollisionWithDifferentFile_doesNotOverwrite`() = runTest {
        val existing = "keep me".toByteArray()
        handler.files["$MEM_SCHEME/dest/a.txt"] = existing
        val data = "new content here".toByteArray()
        val src = source("a.txt", data)

        engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.COPY, RecordingListener())

        assertArrayEquals(existing, handler.files["$MEM_SCHEME/dest/a.txt"])
        assertArrayEquals(data, handler.files["$MEM_SCHEME/dest/a (1).txt"])
    }

    @Test
    fun `transfer_resumesRenamedTempWithoutOrphaning`() = runTest {
        // A different file owns "a.txt", so this copy renames to "a (1).txt". A prior run left its temp.
        handler.files["$MEM_SCHEME/dest/a.txt"] = "other".toByteArray()
        val data = "0123456789".toByteArray()
        val src = source("a.txt", data)
        handler.files["$MEM_SCHEME/dest/a (1).txt$part"] = "01234".toByteArray()

        engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.COPY, RecordingListener())

        // Resumed into the existing temp: final is complete and no stray ".ibexpart" is left behind.
        assertArrayEquals(data, handler.files["$MEM_SCHEME/dest/a (1).txt"])
        assertNull(handler.files["$MEM_SCHEME/dest/a (1).txt$part"])
        assertNull(handler.files["$MEM_SCHEME/dest/a (2).txt"])
    }

    @Test
    fun `transfer_cancelMidFile_throwsAndDeletesTemp`() = runTest {
        val data = ByteArray(200_000) { it.toByte() }
        val src = source("big.bin", data)
        val listener = RecordingListener(cancelAfterBytes = 1)

        var thrown = false
        try {
            engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.COPY, listener)
        } catch (e: TransferCancelledException) {
            thrown = true
        }

        assertTrue(thrown)
        assertNull(handler.files["$MEM_SCHEME/dest/big.bin"])
        assertNull(handler.files["$MEM_SCHEME/dest/big.bin$part"])
    }

    @Test
    fun `transfer_pauseMidFile_throwsAndKeepsTempForResume`() = runTest {
        val data = ByteArray(200_000) { it.toByte() }
        val src = source("big.bin", data)
        val listener = RecordingListener(pauseAfterBytes = 1)

        var thrown = false
        try {
            engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.COPY, listener)
        } catch (e: TransferPausedException) {
            thrown = true
        }

        assertTrue(thrown)
        // Unlike cancel, the partial temp survives so resume can continue from it.
        assertNull(handler.files["$MEM_SCHEME/dest/big.bin"])
        assertTrue((handler.files["$MEM_SCHEME/dest/big.bin$part"]?.size ?: 0) > 0)
    }

    @Test
    fun `transfer_resumeAfterPause_completesFromKeptTemp`() = runTest {
        val data = ByteArray(200_000) { it.toByte() }
        val src = source("big.bin", data)
        // First pass pauses partway, leaving a temp behind.
        runCatching {
            engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.COPY, RecordingListener(pauseAfterBytes = 1))
        }
        val partialTemp = handler.files["$MEM_SCHEME/dest/big.bin$part"]?.size ?: 0
        assertTrue(partialTemp in 1 until data.size)

        // Second pass runs to the end and promotes the temp to the final file.
        engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.COPY, RecordingListener())

        assertArrayEquals(data, handler.files["$MEM_SCHEME/dest/big.bin"])
        assertNull(handler.files["$MEM_SCHEME/dest/big.bin$part"])
    }

    @Test
    fun `transfer_reportsFileStartWithSize`() = runTest {
        val data = "hello world".toByteArray()
        val src = source("a.txt", data)
        val listener = RecordingListener()

        engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.COPY, listener)

        assertEquals(1, listener.fileStarts)
        assertEquals("a.txt", listener.lastFileName)
        assertEquals(data.size.toLong(), listener.lastFileSize)
    }

    @Test
    fun `transfer_sameHandlerMove_usesInstantRenameNoTemp`() = runTest {
        val data = "move me".toByteArray()
        val src = source("a.txt", data)

        engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.MOVE, RecordingListener())

        assertEquals(1, handler.moveCalls)
        assertArrayEquals(data, handler.files["$MEM_SCHEME/dest/a.txt"])
        assertNull(handler.files["$MEM_SCHEME/src/a.txt"])
        assertNull(handler.files["$MEM_SCHEME/dest/a.txt$part"])
    }

    @Test
    fun `transfer_crossHandlerMove_copiesButDoesNotDeleteSource`() = runTest {
        val remote = InMemoryProtocolHandler("smb:/")
        val crossEngine = TransferEngine(setOf(handler, remote), UnconfinedTestDispatcher())
        remote.files["smb://src/a.txt"] = "hi".toByteArray()
        val src = TransferSource("smb://src/a.txt", "a.txt", 2L, false)

        val relocated = crossEngine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.MOVE, RecordingListener())

        // Copied (not renamed), and the engine leaves the source for the caller to delete after verifying.
        assertFalse(relocated)
        assertArrayEquals("hi".toByteArray(), handler.files["$MEM_SCHEME/dest/a.txt"])
        assertArrayEquals("hi".toByteArray(), remote.files["smb://src/a.txt"])
    }

    @Test
    fun `transfer_sizeMismatch_throws`() = runTest {
        // Claimed size (100) does not match the real 10 bytes -> verification must fail.
        handler.files["$MEM_SCHEME/src/a.txt"] = "0123456789".toByteArray()
        val src = TransferSource("$MEM_SCHEME/src/a.txt", "a.txt", 100L, false)

        var thrown = false
        try {
            engine.transfer(src, "$MEM_SCHEME/dest", ClipboardOperation.COPY, RecordingListener())
        } catch (e: IOException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    private class RecordingListener(
        private val cancelAfterBytes: Long = Long.MAX_VALUE,
        private val pauseAfterBytes: Long = Long.MAX_VALUE,
    ) : TransferListener {
        var bytes = 0L
        var filesComplete = 0
        var fileStarts = 0
        var lastFileName: String? = null
        var lastFileSize = -1L
        override fun isCancelled(): Boolean = bytes >= cancelAfterBytes
        override fun isPaused(): Boolean = bytes >= pauseAfterBytes
        override suspend fun onFileStart(name: String, size: Long) {
            fileStarts += 1
            lastFileName = name
            lastFileSize = size
        }
        override suspend fun onBytes(name: String, delta: Long) {
            bytes += delta
        }
        override suspend fun onFileComplete() {
            filesComplete += 1
        }
    }
}
