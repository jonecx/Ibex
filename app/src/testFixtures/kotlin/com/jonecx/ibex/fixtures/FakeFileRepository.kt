package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class FakeFileRepository : FileRepository {
    var filesToReturn: List<FileItem> = emptyList()
    var errorToThrow: Throwable? = null

    override fun getFiles(path: String): Flow<List<FileItem>> = flow {
        errorToThrow?.let { throw it }
        emit(filesToReturn)
    }

    override fun getStorageRoots(): Flow<List<FileItem>> = flowOf(emptyList())

    override suspend fun getFileDetails(path: String): FileItem? = null
}
