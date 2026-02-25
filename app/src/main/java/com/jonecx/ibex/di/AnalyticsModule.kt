package com.jonecx.ibex.di

import com.jonecx.ibex.analytics.AnalyticsProvider
import com.jonecx.ibex.analytics.PostHogAnalyticsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsProvider(impl: PostHogAnalyticsProvider): AnalyticsProvider
}
