package com.jonecx.ibex.di

import com.jonecx.azmaree.player.model.PlayerTelemetry
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.analytics.AnalyticsProvider
import com.jonecx.ibex.analytics.AnalyticsTree
import com.jonecx.ibex.analytics.AxiomMetricsProvider
import com.jonecx.ibex.analytics.CrashReporter
import com.jonecx.ibex.analytics.MetricsProvider
import com.jonecx.ibex.analytics.PostHogAnalyticsProvider
import com.jonecx.ibex.analytics.SentryCrashReporter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val analyticsModule = module {
    single<AnalyticsProvider> { PostHogAnalyticsProvider(androidContext(), get()) }
    single<MetricsProvider> { AxiomMetricsProvider(androidContext(), get()) }
    single<CrashReporter> { SentryCrashReporter(androidContext()) }
    single {
        AnalyticsManager(androidContext(), get(), get(), get(), get(), get(ApplicationScope), get())
    }
    single { AnalyticsTree(get()) }
    // The telemetry sink handed to every embedded AzmareePlayer.
    single<PlayerTelemetry> { get<AnalyticsManager>().playerTelemetry }
}
