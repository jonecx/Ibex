package com.jonecx.ibex.di

import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.analytics.AnalyticsProvider
import com.jonecx.ibex.analytics.AnalyticsTree
import com.jonecx.ibex.analytics.PostHogAnalyticsProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val analyticsModule = module {
    single<AnalyticsProvider> {
        PostHogAnalyticsProvider(androidContext(), get(), get(ApplicationScope), get())
    }
    single { AnalyticsManager(androidContext(), get(), get()) }
    single { AnalyticsTree(get()) }
}
