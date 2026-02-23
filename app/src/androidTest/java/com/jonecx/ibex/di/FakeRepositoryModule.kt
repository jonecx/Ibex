package com.jonecx.ibex.di

import com.jonecx.ibex.data.repository.FakeFileRepository
import com.jonecx.ibex.fixtures.FakeFileRepositoryFactory
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
}
