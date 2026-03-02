package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.repository.FileTrashManager

class FakeFileTrashManager(
    var shouldSucceed: Boolean = true,
) : FileTrashManager {

    val trashedFiles = mutableListOf<FileItem>()

    override suspend fun trashFile(fileItem: FileItem): Boolean {
        if (shouldSucceed) {
            trashedFiles.add(fileItem)
        }
        return shouldSucceed
    }
}
