package com.jonecx.ibex.di

import com.jonecx.ibex.data.repository.MediaStoreStorageAnalyzer
import com.jonecx.ibex.data.repository.StorageAnalyzer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageAnalyzerModule {

    @Binds
    @Singleton
    abstract fun bindStorageAnalyzer(
        impl: MediaStoreStorageAnalyzer,
    ): StorageAnalyzer
}
