package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.repository.FileRepository
import com.jonecx.ibex.data.repository.MediaType
import com.jonecx.ibex.di.FileRepositoryFactory

class FakeFileRepositoryFactory(
    private val repository: FileRepository,
) : FileRepositoryFactory {
    override fun createLocalFileRepository(): FileRepository = repository
    override fun createMediaFileRepository(mediaType: MediaType): FileRepository = repository
    override fun createAppsRepository(): FileRepository = repository
    override fun createRecentFilesRepository(): FileRepository = repository
    override fun createTrashRepository(): FileRepository = repository
}
