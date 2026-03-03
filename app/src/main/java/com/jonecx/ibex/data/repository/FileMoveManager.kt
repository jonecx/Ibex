package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class ClipboardOperation {
    COPY,
    MOVE,
}

interface FileMoveManager {
    suspend fun moveFile(fileItem: FileItem, destinationDir: String): Boolean
    suspend fun copyFile(fileItem: FileItem, destinationDir: String): Boolean
}

@Singleton
class FileSystemMoveManager @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FileMoveManager {

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

    private suspend fun withSourceAndDestination(
        fileItem: FileItem,
        destinationDir: String,
        action: (source: File, destination: File) -> Boolean,
    ): Boolean = withContext(ioDispatcher) {
        val source = File(fileItem.path)
        if (!source.exists()) return@withContext false
        val destination = File(destinationDir, source.name)
        action(source, destination)
    }
}
