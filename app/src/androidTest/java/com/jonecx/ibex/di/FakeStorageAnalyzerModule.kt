package com.jonecx.ibex.di

import com.jonecx.ibex.data.repository.StorageAnalyzer
import com.jonecx.ibex.fixtures.FakeStorageAnalyzer
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [StorageAnalyzerModule::class],
)
object FakeStorageAnalyzerModule {

    @Provides
    @Singleton
    fun provideFakeStorageAnalyzer(): FakeStorageAnalyzer {
        return FakeStorageAnalyzer()
    }

    @Provides
    @Singleton
    fun provideStorageAnalyzer(
        fake: FakeStorageAnalyzer,
    ): StorageAnalyzer = fake
}
