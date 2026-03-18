package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.util.FileTypeUtils.toFileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.File

interface FileRepository {
    fun getFiles(path: String): Flow<List<FileItem>>

    fun getStorageRoots(): Flow<List<FileItem>> = flowOf(emptyList())

    suspend fun getFileDetails(path: String): FileItem? {
        val file = File(path)
        return if (file.exists()) file.toFileItem() else null
    }
}
