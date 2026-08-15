package com.jonecx.ibex.data.transfer

import com.jonecx.ibex.data.repository.ClipboardOperation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TransferJournalTest {

    private lateinit var file: File
    private lateinit var journal: TransferJournal

    @Before
    fun setup() {
        file = File.createTempFile("journal", ".json").apply { delete() }
        journal = TransferJournal(file, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        file.delete()
        File(file.parentFile, "${file.name}.tmp").delete()
    }

    private fun job(id: String) = TransferJob(
        id = id,
        operation = ClipboardOperation.COPY,
        sources = listOf(TransferSource("/src/a.txt", "a.txt", 10L, false)),
        destinationDir = "/dest",
        createdAt = 1L,
        totalBytes = 10L,
        totalFiles = 1,
    )

    @Test
    fun `save_thenLoad_roundTripsJobs`() = runTest {
        val jobs = listOf(job("1"), job("2"))
        journal.save(jobs)

        assertEquals(jobs, journal.load())
    }

    @Test
    fun `load_missingFile_returnsEmpty`() = runTest {
        assertTrue(journal.load().isEmpty())
    }

    @Test
    fun `load_corruptJson_returnsEmpty`() = runTest {
        file.writeText("{ not valid json")

        assertTrue(journal.load().isEmpty())
    }
}
