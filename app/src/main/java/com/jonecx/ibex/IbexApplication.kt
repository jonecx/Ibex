package com.jonecx.ibex

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.logging.AppLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class IbexApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var logger: AppLogger

    override fun onCreate() {
        super.onCreate()
        logger.initialize()
        analyticsManager.initialize()
        logger.i("Ibex application started")
    }

    override fun newImageLoader(): ImageLoader = imageLoader
}
