package com.jonecx.ibex.analytics

import android.content.Context
import androidx.core.content.edit
import com.jonecx.azmaree.player.model.PlayerTelemetry
import com.jonecx.azmaree.player.telemetry.AzmareeAnalytics
import com.jonecx.azmaree.player.telemetry.AzmareeLogger
import com.jonecx.azmaree.player.telemetry.PlayerEvent
import com.jonecx.azmaree.player.telemetry.VideoPrivacy
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Vendor-agnostic telemetry facade: fans app + player events out to behavioral analytics (PostHog),
 * QoE metrics (Axiom), and crash breadcrumbs (Sentry), with one opt-in consent gate over all three.
 */
class AnalyticsManager(
    private val context: Context,
    private val analytics: AnalyticsProvider,
    private val metrics: MetricsProvider,
    private val crashReporter: CrashReporter,
    private val settingsPreferences: SettingsPreferencesContract,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {

    fun initialize() {
        analytics.initialize()
        metrics.initialize()
        crashReporter.initialize()
        // Crash reporting is independent of this opt-in; only behavioral analytics + QoE metrics gate.
        scope.launch {
            settingsPreferences.sendAnalyticsEnabled.collect { granted ->
                analytics.setConsent(granted)
                metrics.setConsent(granted)
            }
        }
        identifyUser()
    }

    fun onNetworkChanged() = analytics.onNetworkChanged()

    fun flush() {
        analytics.flush()
        metrics.flush()
    }

    private fun identifyUser() {
        val userId = getOrCreateUserId()
        analytics.identify(userId)
        logger.d("AnalyticsManager: User identified as $userId")
    }

    private fun getOrCreateUserId(): String {
        val prefs = context.getSharedPreferences("analytics", Context.MODE_PRIVATE)
        var userId = prefs.getString("user_id", null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit { putString("user_id", userId) }
        }
        return userId
    }

    // Behavioral event: PostHog + a Sentry breadcrumb for crash context.
    internal fun capture(event: String, properties: Map<String, Any?> = emptyMap()) {
        val scrubbed = TelemetryScrubber.scrub(properties)
        crashReporter.breadcrumb(category = "analytics", message = event, data = scrubbed)
        analytics.capture(event, scrubbed.filterValues { it != null }.mapValues { it.value!! })
        logger.d("AnalyticsManager: Sent $event")
    }

    // QoE metric: Axiom + a Sentry breadcrumb.
    internal fun trackMetric(event: String, properties: Map<String, Any?> = emptyMap()) {
        crashReporter.breadcrumb(category = "qoe", message = event, data = properties)
        metrics.track(event, properties)
    }

    private fun reportError(message: String, throwable: Throwable?) {
        crashReporter.recordException(throwable, message)
    }

    fun trackScreenView(screenName: String, properties: Map<String, Any> = emptyMap()) {
        val props = mutableMapOf<String, Any>("screen_name" to screenName)
        props.putAll(properties)
        analytics.screen(screenName)
        crashReporter.navigationBreadcrumb(screenName)
        capture("screen_view", props)
    }

    fun trackScreenExit(screenName: String, durationMs: Long) {
        capture(
            "screen_exit",
            mapOf(
                "screen_name" to screenName,
                "duration_ms" to durationMs,
                "duration_seconds" to (durationMs / 1000.0),
            ),
        )
    }

    fun trackTileClick(tileName: String, tileId: String) {
        capture(
            "tile_click",
            mapOf(
                "tile_name" to tileName,
                "tile_id" to tileId,
            ),
        )
    }

    // Routes a log record (from AnalyticsTree) to the right sinks: warnings/errors become
    // behavioral events; errors are also captured by the crash reporter.
    internal fun trackLog(isError: Boolean, properties: Map<String, Any>, message: String, throwable: Throwable?) {
        capture(if (isError) "log_error" else "log_warning", properties)
        if (isError) reportError(message, throwable)
    }

    // The sink handed to every embedded AzmareePlayer: QoE -> Axiom, behavior -> PostHog, every
    // event and log -> Sentry breadcrumbs. Video urls arrive hashed (VideoPrivacy.HASHED).
    val playerTelemetry: PlayerTelemetry by lazy {
        PlayerTelemetry(
            analytics = AzmareeAnalytics { event ->
                when (event.telemetryType) {
                    PlayerEvent.TelemetryType.QOE -> trackMetric(event.eventName, event.properties())
                    PlayerEvent.TelemetryType.BEHAVIOR -> capture(event.eventName, event.properties())
                }
            },
            logger = object : AzmareeLogger {
                override fun log(level: AzmareeLogger.Level, message: String, throwable: Throwable?) {
                    when (level) {
                        AzmareeLogger.Level.DEBUG -> logger.d(message)
                        AzmareeLogger.Level.INFO -> logger.i(message)
                        AzmareeLogger.Level.WARN -> logger.w(message)
                        AzmareeLogger.Level.ERROR -> logger.e(message, throwable)
                    }
                    if (level == AzmareeLogger.Level.ERROR) reportError(message, throwable)
                }
            },
            videoPrivacy = VideoPrivacy.HASHED,
        )
    }
}
