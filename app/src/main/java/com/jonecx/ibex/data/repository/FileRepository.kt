package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getFiles(path: String): Flow<List<FileItem>>
    fun getStorageRoots(): Flow<List<FileItem>>
    suspend fun getFileDetails(path: String): FileItem?
}
