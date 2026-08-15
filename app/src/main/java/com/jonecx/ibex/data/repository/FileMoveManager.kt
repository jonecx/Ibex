package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.util.FileTypeUtils.toFileItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

@Serializable
enum class ClipboardOperation {
    COPY,
    MOVE,
}

// Skips exactly [count] bytes, looping past short skips. Used to resume a stream from an offset.
internal fun InputStream.skipFully(count: Long) {
    var remaining = count
    while (remaining > 0) {
        val skipped = skip(remaining)
        if (skipped <= 0L) {
            if (read() < 0) return
            remaining -= 1
        } else {
            remaining -= skipped
        }
    }
}

interface FileMoveManager {
    suspend fun moveFile(fileItem: FileItem, destinationDir: String): Boolean
    suspend fun copyFile(fileItem: FileItem, destinationDir: String): Boolean
    suspend fun renameFile(fileItem: FileItem, newName: String): Boolean
    suspend fun createFolder(parentDir: String, name: String): Boolean
    suspend fun deleteFile(fileItem: FileItem): Boolean
}

interface ProtocolFileHandler : FileMoveManager {
    fun canHandle(path: String): Boolean
    suspend fun openInputStream(path: String): InputStream
    suspend fun openOutputStream(path: String): OutputStream
    suspend fun listFiles(path: String): List<FileItem>

    // Resumable-transfer seam. Defaults keep existing handlers and cross-protocol copy working unchanged.
    // Reads from [offset]; overriders should seek rather than read-and-discard where the protocol allows.
    suspend fun openInputStream(path: String, offset: Long): InputStream =
        openInputStream(path).also { if (offset > 0) it.skipFully(offset) }

    // Writes starting at [offset]; offset 0 truncates. Overriders must append when offset > 0.
    suspend fun openOutputStream(path: String, offset: Long): OutputStream = openOutputStream(path)

    // Byte length of an existing file, or -1 if it does not exist. Cheap stat used to skip/resume.
    suspend fun sizeOf(path: String): Long = -1L
}

class FileSystemMoveManager(
    private val ioDispatcher: CoroutineDispatcher,
) : ProtocolFileHandler {

    override fun canHandle(path: String): Boolean = !path.contains("://")

    override suspend fun moveFile(fileItem: FileItem, destinationDir: String): Boolean =
        withSourceAndDestination(fileItem, destinationDir) { source, destination ->
            source.renameTo(destination)
        }

    override suspend fun copyFile(fileItem: FileItem, destinationDir: String): Boolean =
        withSourceAndDestination(fileItem, destinationDir) { source, destination ->
            try {
                if (source.isDirectory) {
                    source.copyRecursively(destination, overwrite = false)
                } else {
                    source.copyTo(destination, overwrite = false)
                }
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun renameFile(fileItem: FileItem, newName: String): Boolean =
        withSourceAndDestination(fileItem, fileItem.path.substringBeforeLast("/"), newName) { source, destination ->
            source.renameTo(destination)
        }

    override suspend fun createFolder(parentDir: String, name: String): Boolean =
        withContext(ioDispatcher) {
            val folder = File(parentDir, name)
            !folder.exists() && folder.mkdir()
        }

    override suspend fun deleteFile(fileItem: FileItem): Boolean = withContext(ioDispatcher) {
        val file = File(fileItem.path)
        if (!file.exists()) return@withContext false
        if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    override suspend fun openInputStream(path: String): InputStream = withContext(ioDispatcher) {
        FileInputStream(File(path))
    }

    override suspend fun openInputStream(path: String, offset: Long): InputStream = withContext(ioDispatcher) {
        FileInputStream(File(path)).apply { if (offset > 0) channel.position(offset) }
    }

    override suspend fun openOutputStream(path: String): OutputStream = withContext(ioDispatcher) {
        FileOutputStream(File(path))
    }

    // Append when resuming: the temp file already holds exactly [offset] verified bytes.
    override suspend fun openOutputStream(path: String, offset: Long): OutputStream = withContext(ioDispatcher) {
        FileOutputStream(File(path), offset > 0)
    }

    override suspend fun sizeOf(path: String): Long = withContext(ioDispatcher) {
        val file = File(path)
        if (file.exists()) file.length() else -1L
    }

    override suspend fun listFiles(path: String): List<FileItem> = withContext(ioDispatcher) {
        File(path).listFiles()?.map { it.toFileItem(detailed = false) } ?: emptyList()
    }

    private suspend fun withSourceAndDestination(
        fileItem: FileItem,
        destinationDir: String,
        destinationName: String? = null,
        action: (source: File, destination: File) -> Boolean,
    ): Boolean = withContext(ioDispatcher) {
        val source = File(fileItem.path)
        if (!source.exists()) return@withContext false
        val destination = File(destinationDir, destinationName ?: source.name)
        action(source, destination)
    }
}
