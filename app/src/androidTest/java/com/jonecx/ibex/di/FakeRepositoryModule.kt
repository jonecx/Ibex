package com.jonecx.ibex.di

import com.jonecx.ibex.data.repository.FakeFileRepository
import com.jonecx.ibex.data.repository.FileClipboardManager
import com.jonecx.ibex.data.repository.FileMoveManager
import com.jonecx.ibex.data.repository.FileTrashManager
import com.jonecx.ibex.fixtures.FakeFileClipboardManager
import com.jonecx.ibex.fixtures.FakeFileMoveManager
import com.jonecx.ibex.fixtures.FakeFileRepositoryFactory
import com.jonecx.ibex.fixtures.FakeFileTrashManager
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
        return FakeFileRepositoryFactory(FakeFileRepository())
    }

    @Provides
    @Singleton
    fun provideFileTrashManager(): FileTrashManager {
        return FakeFileTrashManager()
    }

    @Provides
    @Singleton
    fun provideFileMoveManager(): FileMoveManager {
        return FakeFileMoveManager()
    }

    @Provides
    @Singleton
    fun provideFileClipboardManager(fileMoveManager: FileMoveManager): FileClipboardManager {
        return FakeFileClipboardManager(fileMoveManager)
    }
}
