package com.jonecx.ibex.analytics

import android.content.Context
import com.jonecx.ibex.BuildConfig
import com.jonecx.ibex.logging.AppLogger
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * PostHog backend for behavioral analytics (QoE goes to Axiom) behind [AnalyticsProvider].
 * No-ops entirely when POSTHOG_API_KEY is absent, or while consent is off.
 */
class PostHogAnalyticsProvider(
    private val context: Context,
    private val logger: AppLogger,
) : AnalyticsProvider {

    private val consent = AtomicBoolean(false)
    private var enabled = false

    // PostHog.flush() uploads synchronously on the calling thread; keep it off the main thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun initialize() {
        if (BuildConfig.POSTHOG_API_KEY.isEmpty()) {
            logger.w("PostHog API key not configured")
            return
        }
        val config = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host = BuildConfig.POSTHOG_HOST,
        ).apply {
            debug = BuildConfig.DEBUG
            // Ship in batches of 20 or every 30s; queue survives offline up to 1000 events.
            flushAt = 20
            flushIntervalSeconds = 30
            maxQueueSize = 1000
            maxBatchSize = 50
            captureApplicationLifecycleEvents = true
            captureDeepLinks = true
            // Screens are tracked manually from compose navigation, not Activity names.
            captureScreenViews = false
            // Feature flags and surveys are unused; skip their startup network calls.
            preloadFeatureFlags = false
            remoteConfig = false
            sendFeatureFlagEvent = false
        }
        PostHogAndroid.setup(context, config)
        registerSuperProperties()
        enabled = true
        logger.d("PostHog initialized with host: ${BuildConfig.POSTHOG_HOST}")
    }

    override fun identify(userId: String) {
        if (!enabled || !consent.get()) return
        PostHog.identify(distinctId = userId)
    }

    override fun capture(event: String, properties: Map<String, Any>) {
        if (!enabled || !consent.get()) return
        PostHog.capture(event = event, properties = properties)
    }

    override fun screen(name: String) {
        if (!enabled || !consent.get()) return
        PostHog.screen(screenTitle = name)
    }

    override fun setConsent(granted: Boolean) {
        consent.set(granted)
        if (!enabled) return
        if (granted) PostHog.optIn() else PostHog.optOut()
    }

    override fun onNetworkChanged() {
        if (!enabled) return
        PostHog.register("network_type", NetworkContext.type().wire)
    }

    override fun flush() {
        if (!enabled) return
        scope.launch { PostHog.flush() }
    }

    // Super properties stamped onto every event beyond PostHog's built-in device context.
    private fun registerSuperProperties() {
        PostHog.register("app", "ibex")
        PostHog.register("app_build_type", if (BuildConfig.DEBUG) "debug" else "release")
        PostHog.register("network_type", NetworkContext.type().wire)
    }
}
