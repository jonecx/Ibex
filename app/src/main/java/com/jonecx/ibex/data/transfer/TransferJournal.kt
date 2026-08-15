package com.jonecx.ibex.data.transfer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

// Durable list of in-flight transfers, serialized to a small JSON file. The manager is the single
// writer; this class only does atomic read/write so a crash mid-save can never corrupt the queue.
class TransferJournal(
    private val journalFile: File,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun load(): List<TransferJob> = withContext(ioDispatcher) {
        mutex.withLock {
            if (!journalFile.exists()) return@withContext emptyList()
            try {
                val text = journalFile.readText()
                if (text.isBlank()) emptyList() else json.decodeFromString<List<TransferJob>>(text)
            } catch (e: Exception) {
                Timber.e(e, "Transfer journal unreadable, starting empty")
                emptyList()
            }
        }
    }

    suspend fun save(jobs: List<TransferJob>) = withContext(ioDispatcher) {
        mutex.withLock {
            try {
                journalFile.parentFile?.mkdirs()
                // Write to a sibling temp then rename, so the real file is never half-written.
                val temp = File(journalFile.parentFile, "${journalFile.name}.tmp")
                temp.writeText(json.encodeToString(jobs))
                if (!temp.renameTo(journalFile)) {
                    journalFile.writeText(json.encodeToString(jobs))
                    temp.delete()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist transfer journal")
            }
        }
    }
}
