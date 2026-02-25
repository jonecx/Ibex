package com.jonecx.ibex

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.analytics.AnalyticsTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class IbexApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var analyticsTree: AnalyticsTree

    override fun onCreate() {
        super.onCreate()
        analyticsManager.initialize()
        initTimber()
    }

    private fun initTimber() {
//        if (BuildConfig.DEBUG) {
//            Timber.plant(Timber.DebugTree())
//        }
        Timber.plant(analyticsTree)
        Timber.i("Ibex application started")
    }

    override fun newImageLoader(): ImageLoader = imageLoader
}
