package com.jonecx.ibex.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DispatcherModule::class],
)
object TestDispatcherModule {

    @IoDispatcher
    @Provides
    fun provideTestIoDispatcher(): CoroutineDispatcher = UnconfinedTestDispatcher()

    @MainDispatcher
    @Provides
    fun provideTestMainDispatcher(): CoroutineDispatcher = UnconfinedTestDispatcher()

    @DefaultDispatcher
    @Provides
    fun provideTestDefaultDispatcher(): CoroutineDispatcher = UnconfinedTestDispatcher()
}
