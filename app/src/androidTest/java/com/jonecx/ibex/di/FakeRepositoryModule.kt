package com.jonecx.ibex.di

import com.jonecx.ibex.data.repository.FakeFileRepository
import com.jonecx.ibex.data.repository.FileRepository
import com.jonecx.ibex.data.repository.MediaType
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class],
)
object FakeRepositoryModule {

    @Provides
    @Singleton
    fun provideFileRepositoryFactory(): FileRepositoryFactory {
        return object : FileRepositoryFactory {
            private val fakeRepository = FakeFileRepository()
            override fun createLocalFileRepository(): FileRepository = fakeRepository
            override fun createMediaFileRepository(mediaType: MediaType): FileRepository = fakeRepository
            override fun createAppsRepository(): FileRepository = fakeRepository
            override fun createRecentFilesRepository(): FileRepository = fakeRepository
            override fun createTrashRepository(): FileRepository = fakeRepository
        }
    }
}
