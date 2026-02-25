package com.jonecx.ibex.analytics

import android.content.Context
import com.jonecx.ibex.BuildConfig
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.di.ApplicationScope
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class PostHogAnalyticsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsPreferences: SettingsPreferencesContract,
    @ApplicationScope private val scope: CoroutineScope,
) : AnalyticsProvider {

    private val isAnalyticsEnabled = AtomicBoolean(false)

    override fun initialize() {
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
        PostHogAndroid.setup(context, config)
        Timber.d("PostHog initialized with host: ${BuildConfig.POSTHOG_HOST}")

        scope.launch {
            settingsPreferences.sendAnalyticsEnabled.collect { enabled ->
                isAnalyticsEnabled.set(enabled)
            }
        }
    }

    override fun identify(userId: String) {
        PostHog.identify(userId)
    }

    override fun capture(event: String, properties: Map<String, Any>) {
        if (!isAnalyticsEnabled.get()) return
        PostHog.capture(event, null, properties)
    }
}
