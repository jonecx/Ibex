package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.di.IoDispatcher
import com.jonecx.ibex.util.FileTypeUtils
import com.jonecx.ibex.util.FileTypeUtils.toFileItem
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class SmbFileMoveManager @Inject constructor(
    private val smbContextProvider: SmbContextProviderContract,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
                Timber.e(e, "SMB createFolder failed: $parentDir/$name")
                false
            }
        }

    override suspend fun deleteFile(fileItem: FileItem): Boolean =
        withSmbSource(fileItem, "delete") { source, _ ->
            if (fileItem.isDirectory) deleteDirectoryRecursive(source) else source.delete()
            true
        }

    override suspend fun openInputStream(path: String): InputStream = withContext(ioDispatcher) {
        val context = contextForPath(path)
            ?: throw IllegalStateException("No SMB context for path: $path")
        SmbFile(path, context).inputStream
    }

    override suspend fun openOutputStream(path: String): OutputStream = withContext(ioDispatcher) {
        val context = contextForPath(path)
            ?: throw IllegalStateException("No SMB context for path: $path")
        SmbFile(path, context).outputStream
    }

    override suspend fun listFiles(path: String): List<FileItem> = withContext(ioDispatcher) {
        val context = contextForPath(path) ?: return@withContext emptyList()
        val dir = SmbFile(FileTypeUtils.smbEnsureTrailingSlash(path), context)
        dir.listFiles()?.map { it.toFileItem() } ?: emptyList()
    }

    private fun contextForPath(path: String): jcifs.CIFSContext? {
        val host = FileTypeUtils.smbExtractHost(path) ?: return null
        return smbContextProvider.get(host)
    }

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
            Timber.e(e, "SMB $operation failed: ${fileItem.path}")
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
