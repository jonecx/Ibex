package com.jonecx.ibex.di

import com.jonecx.ibex.fixtures.FakeAppLogger
import com.jonecx.ibex.logging.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [LoggerModule::class],
)
object FakeLoggerModule {

    @Provides
    @Singleton
    fun provideAppLogger(): AppLogger {
        return FakeAppLogger()
    }
}
