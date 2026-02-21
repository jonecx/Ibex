package com.jonecx.ibex

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
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

    override fun onCreate() {
        super.onCreate()
        initTimber()
        initPostHog()
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(PostHogTree(settingsPreferences, applicationScope))
        Timber.i("Ibex application started")
    }

    private fun initPostHog() {
        if (BuildConfig.POSTHOG_API_KEY.isNotBlank()) {
            val config = PostHogAndroidConfig(
                apiKey = BuildConfig.POSTHOG_API_KEY,
                host = BuildConfig.POSTHOG_HOST,
            ).apply {
                debug = true
            }
            PostHogAndroid.setup(this, config)
            Timber.d("PostHog initialized with host: ${BuildConfig.POSTHOG_HOST}")
        } else {
            Timber.w("PostHog API key not configured")
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
