package com.jonecx.ibex.di

import com.jonecx.ibex.analytics.AnalyticsProvider
import com.jonecx.ibex.fixtures.FakeAnalyticsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AnalyticsModule::class],
)
object FakeAnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsProvider(): AnalyticsProvider {
        return FakeAnalyticsProvider()
    }
}
