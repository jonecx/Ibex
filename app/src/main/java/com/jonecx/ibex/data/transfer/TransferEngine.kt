package com.jonecx.ibex.data.transfer

import android.net.Uri
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.data.repository.ProtocolFileHandler
import com.jonecx.ibex.util.FileTypeUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

// Thrown when the user cancels a single job; distinct from coroutine CancellationException (whole-queue
// stop, e.g. reboot) so the worker knows to bin this job's temp instead of keeping it for resume.
class TransferCancelledException : Exception()

// Thrown when the user pauses a single job. Unlike cancel, the half-written .ibexpart temp is KEPT so a
// later resume continues from the last verified byte; unlike a coroutine stop, only this job is affected.
class TransferPausedException : Exception()

// Callbacks the worker supplies; the engine never touches WorkManager, Room, or UI state.
interface TransferListener {
    // Polled between chunks so a single job can stop without cancelling the worker coroutine.
    fun isCancelled(): Boolean

    // Polled between chunks so a single job can pause (temp kept) without cancelling the worker coroutine.
    fun isPaused(): Boolean

    // A leaf file is about to be copied: [size] is its total, so the sheet can show a per-file bar.
    suspend fun onFileStart(name: String, size: Long)

    // Bytes written since the previous call, for the running file [name]. Worker aggregates + throttles.
    suspend fun onBytes(name: String, delta: Long)

    // One leaf file finished (renamed into place, or skipped because it was already complete).
    suspend fun onFileComplete()
}

// Protocol-agnostic copy/move. Reads and writes only through ProtocolFileHandler, so local<->local,
// device<->SMB (either direction), and future SMB<->SMB all run the exact same path.
class TransferEngine(
    private val handlers: Set<ProtocolFileHandler>,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private companion object {
        const val PART_SUFFIX = ".ibexpart"
        const val MAX_UNIQUE_ATTEMPTS = 999
    }

    private fun handlerFor(path: String): ProtocolFileHandler =
        handlers.firstOrNull { it.canHandle(path) }
            ?: throw IllegalArgumentException("No protocol handler for path: $path")

    // Same protocol + a MOVE means we can try an instant rename (no bytes move on the same volume).
    private fun isRenameMove(source: TransferSource, destinationDir: String, operation: ClipboardOperation) =
        operation == ClipboardOperation.MOVE && handlerFor(source.path) === handlerFor(destinationDir)

    // Copies one top-level source into destinationDir and returns a TransferOutcome: relocated is true when a
    // same-volume MOVE was an instant rename (source already gone); measured is the file/byte count found while
    // walking this source, so the caller gets the total from the copy pass with no separate remote walk.
    suspend fun transfer(
        source: TransferSource,
        destinationDir: String,
        operation: ClipboardOperation,
        conflictPolicy: ConflictPolicy,
        listener: TransferListener,
    ): TransferOutcome {
        val srcHandler = handlerFor(source.path)
        val dstHandler = handlerFor(destinationDir)

        // Pasting an item into the folder it already lives in: never delete or recopy it, and report it as
        // already-relocated so a MOVE does not then delete the original. This also fences OVERWRITE off from
        // ever deleting a source onto itself.
        if (srcHandler === dstHandler &&
            buildChildPath(destinationDir, source.name).trimEnd('/') == source.path.trimEnd('/')
        ) {
            listener.onFileStart(source.name, source.size.coerceAtLeast(0))
            if (source.size > 0L) listener.onBytes(source.name, source.size.coerceAtLeast(0))
            listener.onFileComplete()
            return TransferOutcome(relocated = true, measured = singleUnit(source))
        }

        // The name this top-level item lands under. Only an explicit user choice changes it; the default
        // path never lists the destination here, so an ordinary copy/move pays nothing for conflict handling.
        var targetName = source.name
        if (conflictPolicy != ConflictPolicy.AUTO) {
            val siblings = dstHandler.listFiles(destinationDir).map { it.name }.toSet()
            if (source.name in siblings) {
                when (conflictPolicy) {
                    ConflictPolicy.OVERWRITE ->
                        dstHandler.deleteFile(minimalItem(buildChildPath(destinationDir, source.name), source.isDirectory))
                    ConflictPolicy.RENAME ->
                        targetName = uniqueAmong(siblings, source.name, source.isDirectory)
                    ConflictPolicy.AUTO -> Unit
                }
            }
        }

        // Same-volume MOVE still relocates instantly when the name is unchanged (no collision, or OVERWRITE
        // after the old item was removed). A RENAME to a new name falls through to copy-then-delete.
        if (targetName == source.name &&
            isRenameMove(source, destinationDir, operation) &&
            srcHandler.moveFile(source.toFileItem(), destinationDir)
        ) {
            // The rename relocated the whole subtree at once; count it as a single unit.
            listener.onFileStart(source.name, source.size.coerceAtLeast(0))
            listener.onBytes(source.name, source.size.coerceAtLeast(0))
            listener.onFileComplete()
            return TransferOutcome(relocated = true, measured = singleUnit(source))
        }

        val measured = copyTree(source, destinationDir, targetName, srcHandler, dstHandler, listener)
        return TransferOutcome(relocated = false, measured = measured)
    }

    // Removes a source after its copy has been verified complete by the caller. Never call this
    // speculatively: deleting before the copy is proven is exactly how a bad listing loses data.
    suspend fun deleteSource(source: TransferSource) {
        handlerFor(source.path).deleteFile(source.toFileItem())
    }

    // Copies the subtree and returns what it enumerated (leaf files + their declared bytes). The count comes
    // from this same walk, so a remote directory is listed once, not once to measure and again to copy.
    private suspend fun copyTree(
        source: TransferSource,
        destinationDir: String,
        // Name this item lands under: the resolved name at the top level, the child's own name below.
        targetName: String,
        srcHandler: ProtocolFileHandler,
        dstHandler: ProtocolFileHandler,
        listener: TransferListener,
    ): Measurement {
        if (!source.isDirectory) {
            copyFileResumable(source, destinationDir, targetName, srcHandler, dstHandler, listener)
            listener.onFileComplete()
            return singleUnit(source)
        }
        dstHandler.createFolder(destinationDir, targetName)
        val childDir = buildChildPath(destinationDir, targetName)
        var files = 0
        var bytes = 0L
        for (child in srcHandler.listFiles(source.path)) {
            checkActive(listener)
            val childMeasure = copyTree(child.toTransferSource(), childDir, child.name, srcHandler, dstHandler, listener)
            files += childMeasure.files
            bytes += childMeasure.bytes
        }
        return Measurement(files, bytes)
    }

    private suspend fun copyFileResumable(
        source: TransferSource,
        destinationDir: String,
        targetName: String,
        srcHandler: ProtocolFileHandler,
        dstHandler: ProtocolFileHandler,
        listener: TransferListener,
    ) = withContext(ioDispatcher) {
        val size = source.size
        // Announce the file first so the sheet's per-file bar has a name and a denominator right away.
        listener.onFileStart(source.name, size.coerceAtLeast(0))
        var finalPath = buildChildPath(destinationDir, targetName)

        val existingFinal = dstHandler.sizeOf(finalPath)
        if (existingFinal >= 0L) {
            // Already fully copied (an earlier run finished this file): count it and skip re-reading it.
            if (size >= 0L && existingFinal == size) {
                if (size > 0L) listener.onBytes(source.name, size)
                return@withContext
            }
            // A different file already owns this name. The AUTO path never overwrites: pick a free name.
            finalPath = uniqueName(dstHandler, destinationDir, targetName)
        }

        val tempPath = finalPath + PART_SUFFIX
        var resumeAt = dstHandler.sizeOf(tempPath)
        if (resumeAt < 0L || (size in 0 until resumeAt)) resumeAt = 0L
        if (resumeAt > 0L) listener.onBytes(source.name, resumeAt)

        try {
            srcHandler.openInputStream(source.path, resumeAt).use { input ->
                dstHandler.openOutputStream(tempPath, resumeAt).use { output ->
                    val buffer = ByteArray(FileTypeUtils.IO_BUFFER_SIZE)
                    while (true) {
                        if (listener.isCancelled()) throw TransferCancelledException()
                        // Pause keeps the temp (streams close/flush on the way out), so resume continues from here.
                        if (listener.isPaused()) throw TransferPausedException()
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        listener.onBytes(source.name, read.toLong())
                    }
                    output.flush()
                }
            }
        } catch (e: TransferCancelledException) {
            // User binned this job: drop the half-written temp so nothing junk is left behind.
            runCatching { dstHandler.deleteFile(minimalItem(tempPath)) }
            throw e
        }

        val written = dstHandler.sizeOf(tempPath)
        if (size >= 0L && written != size) {
            throw IOException("Size mismatch for ${source.name}: expected $size, wrote $written")
        }
        // Promote the verified temp to its real name (atomic rename within the same directory).
        if (!dstHandler.renameFile(minimalItem(tempPath), finalPath.substringAfterLast('/'))) {
            throw IOException("Failed to finalize ${source.name}")
        }
    }

    private suspend fun uniqueName(
        dstHandler: ProtocolFileHandler,
        destinationDir: String,
        name: String,
    ): String {
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        // Pick the first name whose FINAL file is free. A leftover ".ibexpart" is fine to reuse here:
        // it's this same file's own interrupted copy, so resuming into it avoids orphaning a temp.
        // Jobs run one file at a time, so no other file competes for the same candidate.
        for (i in 1..MAX_UNIQUE_ATTEMPTS) {
            val candidate = buildChildPath(destinationDir, "$base ($i)$ext")
            if (dstHandler.sizeOf(candidate) < 0L) return candidate
        }
        throw IOException("Could not find a free name for $name in $destinationDir")
    }

    // First free "name (n)" not already among the destination's children. Works for files and directories
    // (directories keep their whole name, files split off the extension), for the RENAME conflict policy.
    private fun uniqueAmong(siblings: Set<String>, name: String, isDirectory: Boolean): String {
        val dot = if (isDirectory) -1 else name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        for (i in 1..MAX_UNIQUE_ATTEMPTS) {
            val candidate = "$base ($i)$ext"
            if (candidate !in siblings) return candidate
        }
        throw IOException("Could not find a free name for $name")
    }

    private suspend fun checkActive(listener: TransferListener) {
        if (listener.isCancelled()) throw TransferCancelledException()
        if (listener.isPaused()) throw TransferPausedException()
        coroutineContext.ensureActive()
    }

    // One item counted as a single unit: a leaf file, or a whole subtree relocated by an instant rename.
    private fun singleUnit(source: TransferSource): Measurement =
        Measurement(files = 1, bytes = source.size.coerceAtLeast(0))

    private fun buildChildPath(parentDir: String, name: String): String =
        "${parentDir.trimEnd('/')}/$name"

    private fun minimalItem(path: String, isDirectory: Boolean = false): FileItem = FileItem(
        name = path.substringAfterLast('/'),
        path = path,
        uri = Uri.EMPTY,
        size = 0L,
        lastModified = 0L,
        isDirectory = isDirectory,
        fileType = if (isDirectory) FileType.DIRECTORY else FileType.UNKNOWN,
    )

    private fun TransferSource.toFileItem(): FileItem = minimalItem(path, isDirectory)

    private fun FileItem.toTransferSource(): TransferSource =
        TransferSource(path = path, name = name, size = size, isDirectory = isDirectory)

    data class Measurement(val files: Int, val bytes: Long)

    // Result of transferring one top-level source: whether an instant rename relocated it, plus the
    // file/byte count discovered during the copy so the caller needs no separate measure walk.
    data class TransferOutcome(val relocated: Boolean, val measured: Measurement)
}
