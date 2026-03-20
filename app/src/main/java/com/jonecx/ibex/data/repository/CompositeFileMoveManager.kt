package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.di.IoDispatcher
import com.jonecx.ibex.util.FileTypeUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompositeFileMoveManager @Inject constructor(
    private val handlers: Set<@JvmSuppressWildcards ProtocolFileHandler>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FileMoveManager {

    private fun handlerFor(path: String): ProtocolFileHandler =
        handlers.firstOrNull { it.canHandle(path) }
            ?: throw IllegalArgumentException("No protocol handler for path: $path")

    override suspend fun moveFile(fileItem: FileItem, destinationDir: String): Boolean {
        val srcHandler = handlerFor(fileItem.path)
        val dstHandler = handlerFor(destinationDir)
        if (srcHandler === dstHandler) return srcHandler.moveFile(fileItem, destinationDir)
        return crossProtocolCopy(fileItem, destinationDir, srcHandler, dstHandler) &&
            srcHandler.deleteFile(fileItem)
    }

    override suspend fun copyFile(fileItem: FileItem, destinationDir: String): Boolean {
        val srcHandler = handlerFor(fileItem.path)
        val dstHandler = handlerFor(destinationDir)
        if (srcHandler === dstHandler) return srcHandler.copyFile(fileItem, destinationDir)
        return crossProtocolCopy(fileItem, destinationDir, srcHandler, dstHandler)
    }

    override suspend fun renameFile(fileItem: FileItem, newName: String): Boolean =
        handlerFor(fileItem.path).renameFile(fileItem, newName)

    override suspend fun createFolder(parentDir: String, name: String): Boolean =
        handlerFor(parentDir).createFolder(parentDir, name)

    override suspend fun deleteFile(fileItem: FileItem): Boolean =
        handlerFor(fileItem.path).deleteFile(fileItem)

    private suspend fun crossProtocolCopy(
        fileItem: FileItem,
        destinationDir: String,
        srcHandler: ProtocolFileHandler,
        dstHandler: ProtocolFileHandler,
    ): Boolean = withContext(ioDispatcher) {
        try {
            if (fileItem.isDirectory) {
                crossProtocolCopyDirectory(fileItem, destinationDir, srcHandler, dstHandler)
            } else {
                crossProtocolCopyFile(fileItem.path, fileItem.name, destinationDir, srcHandler, dstHandler)
            }
        } catch (e: Exception) {
            Timber.e(e, "Cross-protocol copy failed: ${fileItem.path} -> $destinationDir")
            false
        }
    }

    private suspend fun crossProtocolCopyFile(
        sourcePath: String,
        fileName: String,
        destinationDir: String,
        srcHandler: ProtocolFileHandler,
        dstHandler: ProtocolFileHandler,
    ): Boolean {
        val destPath = buildChildPath(destinationDir, fileName)
        srcHandler.openInputStream(sourcePath).use { input ->
            dstHandler.openOutputStream(destPath).use { output ->
                input.copyTo(output, FileTypeUtils.IO_BUFFER_SIZE)
            }
        }
        return true
    }

    private suspend fun crossProtocolCopyDirectory(
        fileItem: FileItem,
        destinationDir: String,
        srcHandler: ProtocolFileHandler,
        dstHandler: ProtocolFileHandler,
    ): Boolean {
        dstHandler.createFolder(destinationDir, fileItem.name)
        val newDestDir = buildChildPath(destinationDir, fileItem.name)

        val children = srcHandler.listFiles(fileItem.path)
        children.forEach { child ->
            if (child.isDirectory) {
                crossProtocolCopyDirectory(child, newDestDir, srcHandler, dstHandler)
            } else {
                crossProtocolCopyFile(child.path, child.name, newDestDir, srcHandler, dstHandler)
            }
        }
        return true
    }

    private fun buildChildPath(parentDir: String, name: String): String =
        "${parentDir.trimEnd('/')}/$name"
}
