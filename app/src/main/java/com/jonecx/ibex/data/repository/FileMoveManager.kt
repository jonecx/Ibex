package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.di.IoDispatcher
import com.jonecx.ibex.util.FileTypeUtils.toFileItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class ClipboardOperation {
    COPY,
    MOVE,
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
}

@Singleton
class FileSystemMoveManager @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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

    override suspend fun openOutputStream(path: String): OutputStream = withContext(ioDispatcher) {
        FileOutputStream(File(path))
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
