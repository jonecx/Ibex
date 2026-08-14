package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.repository.FileRepository
import com.jonecx.ibex.data.repository.MediaType
import com.jonecx.ibex.di.FileRepositoryFactory

class FakeFileRepositoryFactory(
    private val repository: FileRepository,
) : FileRepositoryFactory {
    // Records which repository a source resolved to, so tests can pin the Images/Videos folder-browsing wiring.
    var lastMediaFileType: MediaType? = null
    var lastMediaFolderType: MediaType? = null

    override fun createLocalFileRepository(): FileRepository = repository
    override fun createMediaFileRepository(mediaType: MediaType): FileRepository =
        repository.also { lastMediaFileType = mediaType }

    override fun createMediaFolderRepository(mediaType: MediaType): FileRepository =
        repository.also { lastMediaFolderType = mediaType }

    override fun createAppsRepository(): FileRepository = repository
    override fun createRecentFilesRepository(): FileRepository = repository
    override fun createTrashRepository(): FileRepository = repository
    override fun createSmbFileRepository(connectionId: String): FileRepository = repository
}
