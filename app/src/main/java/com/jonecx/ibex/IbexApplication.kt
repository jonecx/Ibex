package com.jonecx.ibex

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.jonecx.ibex.analytics.PostHogTree
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.di.ApplicationScope
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class IbexApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var settingsPreferences: SettingsPreferencesContract

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        initTimber()
        initPostHog()
    }

    private fun initTimber() {
//        if (BuildConfig.DEBUG) {
//            Timber.plant(Timber.DebugTree())
//        }
        Timber.plant(PostHogTree(settingsPreferences, applicationScope))
        Timber.i("Ibex application started")
    }

    private fun initPostHog() {
        if (BuildConfig.POSTHOG_API_KEY.isEmpty()) {
            Timber.w("PostHog API key not configured")
            return
        }
        val config = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host = BuildConfig.POSTHOG_HOST,
        ).apply {
            debug = true // replace this with BuildConfig.DEBUG
        }
        PostHogAndroid.setup(this@IbexApplication, config)
        Timber.d("PostHog initialized with host: ${BuildConfig.POSTHOG_HOST}")
    }

    override fun newImageLoader(): ImageLoader = imageLoader
}
