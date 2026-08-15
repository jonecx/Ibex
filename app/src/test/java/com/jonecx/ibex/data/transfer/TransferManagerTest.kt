package com.jonecx.ibex.data.transfer

import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.fixtures.InMemoryProtocolHandler
import com.jonecx.ibex.fixtures.MEM_SCHEME
import com.jonecx.ibex.fixtures.memFileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TransferManagerTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val handler = InMemoryProtocolHandler() // "mem:/" — the local device in these tests
    private val remoteHandler = InMemoryProtocolHandler("smb:/") // a pretend remote source
    private lateinit var journalFile: File
    private lateinit var appScope: CoroutineScope
    private lateinit var scheduler: RecordingScheduler
    private lateinit var manager: DefaultTransferManager

    @Before
    fun setup() {
        journalFile = File.createTempFile("mgr-journal", ".json").apply { delete() }
        appScope = CoroutineScope(dispatcher)
        scheduler = RecordingScheduler()
        manager = DefaultTransferManager(
            scheduler = scheduler,
            engine = TransferEngine(setOf(handler, remoteHandler), dispatcher),
            journal = TransferJournal(journalFile, dispatcher),
            appScope = appScope,
        )
    }

    @After
    fun tearDown() {
        appScope.cancel()
        journalFile.delete()
        File(journalFile.parentFile, "${journalFile.name}.tmp").delete()
    }

    private fun seedSource(name: String, bytes: ByteArray) {
        handler.files["$MEM_SCHEME/src/$name"] = bytes
    }

    private fun sourceItem(name: String) = memFileItem("$MEM_SCHEME/src/$name", handler.files["$MEM_SCHEME/src/$name"]!!.size.toLong())

    @Test
    fun `enqueue_schedulesWorkerAndQueuesJob`() = runTest {
        seedSource("a.txt", "hi".toByteArray())

        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")

        assertEquals(1, scheduler.count)
        assertTrue(manager.snapshot.value.hasActive)
    }

    @Test
    fun `runQueue_copiesFilesAndPrunesOnComplete`() = runTest {
        seedSource("a.txt", "hello".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")

        manager.runQueue()

        assertArrayEquals("hello".toByteArray(), handler.files["$MEM_SCHEME/dest/a.txt"])
        assertFalse(manager.snapshot.value.hasActive)
    }

    @Test
    fun `runQueue_moveRemovesSource`() = runTest {
        seedSource("a.txt", "move".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.MOVE, "$MEM_SCHEME/dest")

        manager.runQueue()

        assertArrayEquals("move".toByteArray(), handler.files["$MEM_SCHEME/dest/a.txt"])
        assertNull(handler.files["$MEM_SCHEME/src/a.txt"])
    }

    @Test
    fun `cancel_queuedJob_isNeverCopied`() = runTest {
        seedSource("a.txt", "nope".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        val jobId = manager.snapshot.value.jobs.first().id

        manager.cancel(jobId)
        manager.runQueue()

        assertNull(handler.files["$MEM_SCHEME/dest/a.txt"])
        assertFalse(manager.snapshot.value.hasActive)
    }

    @Test
    fun `hasPendingWork_reflectsQueueState`() = runTest {
        assertFalse(manager.hasPendingWork())

        seedSource("a.txt", "x".toByteArray())
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")
        assertTrue(manager.hasPendingWork())

        manager.runQueue()
        assertFalse(manager.hasPendingWork())
    }

    @Test
    fun `runQueue_emitsCompletionForDestination`() = runTest {
        seedSource("a.txt", "done".toByteArray())
        val completions = mutableListOf<String>()
        val job = launch(dispatcher) { manager.completions.collect { completions.add(it) } }
        manager.enqueue(listOf(sourceItem("a.txt")), ClipboardOperation.COPY, "$MEM_SCHEME/dest")

        manager.runQueue()

        assertTrue(completions.contains("$MEM_SCHEME/dest"))
        job.cancel()
    }

    @Test
    fun `recoverAndResume_loadsJournalAndResumes`() = runTest {
        seedSource("a.txt", "resumed".toByteArray())
        // Pre-populate the journal as if a prior process left a queued job behind.
        TransferJournal(journalFile, dispatcher).save(
            listOf(
                TransferJob(
                    id = "job-1",
                    operation = ClipboardOperation.COPY,
                    sources = listOf(TransferSource("$MEM_SCHEME/src/a.txt", "a.txt", 7L, false)),
                    destinationDir = "$MEM_SCHEME/dest",
                    createdAt = 1L,
                    status = TransferStatus.QUEUED,
                ),
            ),
        )

        manager.recoverAndResume()
        assertEquals(1, scheduler.count)

        manager.runQueue()
        assertArrayEquals("resumed".toByteArray(), handler.files["$MEM_SCHEME/dest/a.txt"])
    }

    @Test
    fun `runQueue_crossHandlerMove_deletesSourceAfterFullCopy`() = runTest {
        remoteHandler.dirs.add("smb://src/Folder")
        remoteHandler.files["smb://src/Folder/f1.txt"] = "one".toByteArray()
        remoteHandler.files["smb://src/Folder/f2.txt"] = "two".toByteArray()
        val folder = memFileItem("smb://src/Folder", 0L, isDirectory = true)

        manager.enqueue(listOf(folder), ClipboardOperation.MOVE, "$MEM_SCHEME/dest")
        manager.runQueue()

        // Both files copied to the local destination.
        assertArrayEquals("one".toByteArray(), handler.files["$MEM_SCHEME/dest/Folder/f1.txt"])
        assertArrayEquals("two".toByteArray(), handler.files["$MEM_SCHEME/dest/Folder/f2.txt"])
        // Source removed only after the whole copy verified.
        assertNull(remoteHandler.files["smb://src/Folder/f1.txt"])
        assertNull(remoteHandler.files["smb://src/Folder/f2.txt"])
    }

    @Test
    fun `runQueue_crossHandlerMove_whenCopyFails_keepsSourceIntact`() = runTest {
        remoteHandler.dirs.add("smb://src/Folder")
        remoteHandler.files["smb://src/Folder/f1.txt"] = "one".toByteArray()
        remoteHandler.files["smb://src/Folder/f2.txt"] = "two".toByteArray()
        // The second file cannot be read: the move must fail and never delete the source.
        remoteHandler.failReadPaths.add("smb://src/Folder/f2.txt")
        val folder = memFileItem("smb://src/Folder", 0L, isDirectory = true)

        manager.enqueue(listOf(folder), ClipboardOperation.MOVE, "$MEM_SCHEME/dest")
        manager.runQueue()

        // No data loss: both source files remain.
        assertArrayEquals("one".toByteArray(), remoteHandler.files["smb://src/Folder/f1.txt"])
        assertArrayEquals("two".toByteArray(), remoteHandler.files["smb://src/Folder/f2.txt"])
    }

    @Test
    fun `runQueue_crossHandlerMove_whenListingFails_keepsSourceIntact`() = runTest {
        remoteHandler.dirs.add("smb://src/Folder")
        remoteHandler.files["smb://src/Folder/f1.txt"] = "one".toByteArray()
        // The folder cannot be enumerated: the move must fail, never treat it as empty and delete it.
        remoteHandler.failListPaths.add("smb://src/Folder")
        val folder = memFileItem("smb://src/Folder", 0L, isDirectory = true)

        manager.enqueue(listOf(folder), ClipboardOperation.MOVE, "$MEM_SCHEME/dest")
        manager.runQueue()

        // No phantom job stuck in the bar, and the source is fully intact.
        assertFalse(manager.snapshot.value.hasActive)
        assertArrayEquals("one".toByteArray(), remoteHandler.files["smb://src/Folder/f1.txt"])
    }

    private class RecordingScheduler : TransferScheduler {
        var count = 0
        override fun ensureRunning() {
            count += 1
        }
    }
}
