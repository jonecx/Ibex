package com.jonecx.ibex

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.jonecx.azmaree.player.AzmareePlayers
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.analytics.NetworkContext
import com.jonecx.ibex.data.transfer.TransferManager
import com.jonecx.ibex.di.appModules
import com.jonecx.ibex.logging.AppLogger
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class IbexApplication : Application(), ImageLoaderFactory {

    private val imageLoader: ImageLoader by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private val logger: AppLogger by inject()
    private val transferManager: TransferManager by inject()

    override fun onCreate() {
        super.onCreate()
        val startMs = System.currentTimeMillis()
        startKoin {
            androidContext(this@IbexApplication)
            modules(appModules)
        }
        // Init network context before analytics so PostHog/Axiom stamp the transport from the start.
        NetworkContext.init(this) { analyticsManager.onNetworkChanged() }
        logger.initialize()
        analyticsManager.initialize()
        // Pick up any transfer interrupted by a kill or reboot and resume it.
        transferManager.recoverAndResume()
        // Startup-cost QoE (Axiom). App open/background funnels come from PostHog's lifecycle capture.
        analyticsManager.trackAppStart(System.currentTimeMillis() - startMs)
        logger.i("Ibex application started")
    }

    override fun newImageLoader(): ImageLoader = imageLoader

    // Video is a side feature here; hand Azmaree's pooled decoders back when the UI is hidden.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            // Ship the ~30s-batched telemetry before the app is likely evicted.
            analyticsManager.flush()
            AzmareePlayers.release()
        }
    }
}
