package com.jonecx.ibex

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.di.appModules
import com.jonecx.ibex.logging.AppLogger
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class IbexApplication : Application(), ImageLoaderFactory {

    private val imageLoader: ImageLoader by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private val logger: AppLogger by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@IbexApplication)
            modules(appModules)
        }
        logger.initialize()
        analyticsManager.initialize()
        logger.i("Ibex application started")
    }

    override fun newImageLoader(): ImageLoader = imageLoader
}
