package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.util.FileTypeUtils
import com.jonecx.ibex.util.FileTypeUtils.toFileItem
import jcifs.smb.SmbFile
import jcifs.smb.SmbFileOutputStream
import jcifs.smb.SmbRandomAccessFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

class SmbFileMoveManager(
    private val smbContextProvider: SmbContextProviderContract,
    private val ioDispatcher: CoroutineDispatcher,
) : ProtocolFileHandler {

    override fun canHandle(path: String): Boolean = path.startsWith(FileTypeUtils.SMB_SCHEME_PREFIX)

    override suspend fun moveFile(fileItem: FileItem, destinationDir: String): Boolean =
        withSmbSource(fileItem, "move") { source, context ->
            val destPath = FileTypeUtils.smbBuildChildPath(destinationDir, fileItem.name, fileItem.isDirectory)
            source.renameTo(SmbFile(destPath, context))
            true
        }

    override suspend fun copyFile(fileItem: FileItem, destinationDir: String): Boolean =
        withSmbSource(fileItem, "copy") { source, context ->
            if (fileItem.isDirectory) {
                copyDirectoryRecursive(source, destinationDir, context)
            } else {
                val destPath = FileTypeUtils.smbBuildChildPath(destinationDir, fileItem.name, false)
                val destination = SmbFile(destPath, context)
                source.inputStream.use { input ->
                    destination.outputStream.use { output ->
                        input.copyTo(output, FileTypeUtils.IO_BUFFER_SIZE)
                    }
                }
                true
            }
        }

    override suspend fun renameFile(fileItem: FileItem, newName: String): Boolean =
        withSmbSource(fileItem, "rename") { source, context ->
            val parentPath = fileItem.path.trimEnd('/').substringBeforeLast('/') + "/"
            val destPath = FileTypeUtils.smbBuildChildPath(parentPath, newName, fileItem.isDirectory)
            source.renameTo(SmbFile(destPath, context))
            true
        }

    override suspend fun createFolder(parentDir: String, name: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val context = contextForPath(parentDir) ?: return@withContext false
                val folderPath = FileTypeUtils.smbBuildChildPath(parentDir, name, true)
                val folder = SmbFile(folderPath, context)
                if (folder.exists()) return@withContext false
                folder.mkdir()
                true
            } catch (e: Exception) {
                Timber.e(e, "SMB createFolder failed")
                false
            }
        }

    override suspend fun deleteFile(fileItem: FileItem): Boolean =
        withSmbSource(fileItem, "delete") { source, _ ->
            if (fileItem.isDirectory) deleteDirectoryRecursive(source) else source.delete()
            true
        }

    override suspend fun openInputStream(path: String): InputStream = withContext(ioDispatcher) {
        SmbFile(path, requireContext(path)).inputStream
    }

    // Resume seeks straight to [offset] via SMB random access instead of read-and-discarding every
    // already-copied byte, so a resumed download never re-fetches what the temp already holds.
    override suspend fun openInputStream(path: String, offset: Long): InputStream = withContext(ioDispatcher) {
        val context = requireContext(path)
        if (offset <= 0L) return@withContext SmbFile(path, context).inputStream
        val randomAccess = SmbFile(path, context).openRandomAccess("r")
        try {
            randomAccess.seek(offset)
        } catch (e: Exception) {
            runCatching { randomAccess.close() }
            throw e
        }
        SmbRandomAccessInputStream(randomAccess)
    }

    override suspend fun openOutputStream(path: String): OutputStream = withContext(ioDispatcher) {
        SmbFile(path, requireContext(path)).outputStream
    }

    // Append when resuming: the temp file already holds exactly [offset] verified bytes.
    override suspend fun openOutputStream(path: String, offset: Long): OutputStream = withContext(ioDispatcher) {
        SmbFileOutputStream(SmbFile(path, requireContext(path)), offset > 0)
    }

    override suspend fun sizeOf(path: String): Long = withContext(ioDispatcher) {
        try {
            val context = contextForPath(path) ?: return@withContext -1L
            val file = SmbFile(path, context)
            if (file.exists()) file.length() else -1L
        } catch (e: Exception) {
            Timber.e(e, "SMB sizeOf failed")
            -1L
        }
    }

    // A copy/move walks the tree through this. It MUST fail loudly, never quietly return empty on error:
    // an empty result reads as "directory has no children", which for a MOVE would delete a source we
    // never actually copied. An empty directory returns an empty array (no throw); a failure throws.
    override suspend fun listFiles(path: String): List<FileItem> = withContext(ioDispatcher) {
        val context = contextForPath(path)
            ?: throw IOException("No SMB context for listing")
        val dir = SmbFile(FileTypeUtils.smbEnsureTrailingSlash(path), context)
        val children = dir.listFiles()
            ?: throw IOException("SMB directory listing failed")
        children.map { it.toFileItem(detailed = false) }
    }

    private fun contextForPath(path: String): jcifs.CIFSContext? {
        val host = FileTypeUtils.smbExtractHost(path) ?: return null
        return smbContextProvider.get(host)
    }

    // Context is required to open any stream: absence is a programming/config error, so fail loudly.
    private fun requireContext(path: String): jcifs.CIFSContext =
        contextForPath(path) ?: throw IllegalStateException("No SMB context for path: $path")

    private suspend fun withSmbSource(
        fileItem: FileItem,
        operation: String,
        action: suspend (source: SmbFile, context: jcifs.CIFSContext) -> Boolean,
    ): Boolean = withContext(ioDispatcher) {
        try {
            val context = contextForPath(fileItem.path) ?: return@withContext false
            val source = SmbFile(FileTypeUtils.smbEnsureTrailingSlash(fileItem.path, fileItem.isDirectory), context)
            action(source, context)
        } catch (e: Exception) {
            Timber.e(e, "SMB $operation failed")
            false
        }
    }

    private suspend fun deleteDirectoryRecursive(dir: SmbFile) {
        dir.listFiles()?.forEach { child ->
            coroutineContext.ensureActive()
            if (child.isDirectory) {
                deleteDirectoryRecursive(child)
            } else {
                child.delete()
            }
        }
        dir.delete()
    }

    private suspend fun copyDirectoryRecursive(
        source: SmbFile,
        destinationDir: String,
        context: jcifs.CIFSContext,
    ): Boolean {
        val newDirPath = FileTypeUtils.smbBuildChildPath(destinationDir, source.name.trimEnd('/'), true)
        val newDir = SmbFile(newDirPath, context)
        newDir.mkdir()

        source.listFiles()?.forEach { child ->
            coroutineContext.ensureActive()
            if (child.isDirectory) {
                copyDirectoryRecursive(child, newDirPath, context)
            } else {
                val destFilePath = FileTypeUtils.smbBuildChildPath(newDirPath, child.name.trimEnd('/'), false)
                val destFile = SmbFile(destFilePath, context)
                child.inputStream.use { input ->
                    destFile.outputStream.use { output ->
                        input.copyTo(output, FileTypeUtils.IO_BUFFER_SIZE)
                    }
                }
            }
        }
        return true
    }
}

// Adapts an already-seeked SmbRandomAccessFile to InputStream so the transfer engine reads it like any
// other stream. Closing the stream closes the underlying random-access handle.
private class SmbRandomAccessInputStream(
    private val randomAccess: SmbRandomAccessFile,
) : InputStream() {
    override fun read(): Int = randomAccess.read()
    override fun read(b: ByteArray, off: Int, len: Int): Int = randomAccess.read(b, off, len)
    override fun close() = randomAccess.close()
}
