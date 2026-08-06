package com.jonecx.ibex.di

import com.jonecx.ibex.ui.viewer.MediaViewerArgs
import org.koin.dsl.module

// Cross-screen holder shared between MainActivity (as a CompositionLocal) and the media viewer.
val coreModule = module {
    single { MediaViewerArgs() }
}

// Aggregated graph loaded by IbexApplication.startKoin.
val appModules = listOf(
    coreModule,
    dispatcherModule,
    analyticsModule,
    cryptoModule,
    loggerModule,
    permissionModule,
    playerModule,
    preferencesModule,
    imageLoaderModule,
    imageRequestModule,
    repositoryModule,
    storageAnalyzerModule,
    viewModelModule,
)
