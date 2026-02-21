package com.jonecx.ibex.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DispatcherModule::class],
)
object TestDispatcherModule {

    @OptIn(ExperimentalCoroutinesApi::class)
    @IoDispatcher
    @Provides
    fun provideTestIoDispatcher(): CoroutineDispatcher = UnconfinedTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @MainDispatcher
    @Provides
    fun provideTestMainDispatcher(): CoroutineDispatcher = UnconfinedTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @DefaultDispatcher
    @Provides
    fun provideTestDefaultDispatcher(): CoroutineDispatcher = UnconfinedTestDispatcher()

    @ApplicationScope
    @Singleton
    @Provides
    fun provideTestApplicationScope(
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}
