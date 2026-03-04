package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.repository.FileMoveManager

class FakeFileMoveManager : FileMoveManager {
    val movedFiles = mutableListOf<Pair<FileItem, String>>()
    val copiedFiles = mutableListOf<Pair<FileItem, String>>()
    var shouldSucceed = true

    override suspend fun moveFile(fileItem: FileItem, destinationDir: String): Boolean {
        movedFiles.add(fileItem to destinationDir)
        return shouldSucceed
    }

    override suspend fun copyFile(fileItem: FileItem, destinationDir: String): Boolean {
        copiedFiles.add(fileItem to destinationDir)
        return shouldSucceed
    }

    val renamedFiles = mutableListOf<Pair<FileItem, String>>()

    override suspend fun renameFile(fileItem: FileItem, newName: String): Boolean {
        renamedFiles.add(fileItem to newName)
        return shouldSucceed
    }

    val createdFolders = mutableListOf<Pair<String, String>>()

    override suspend fun createFolder(parentDir: String, name: String): Boolean {
        createdFolders.add(parentDir to name)
        return shouldSucceed
    }
}
