package com.jonecx.ibex.fixtures

import android.content.Context
import com.jonecx.ibex.analytics.AnalyticsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Bundles a real [AnalyticsManager] wired to recording fakes so ViewModel tests can assert the
 * behavioral events ([captured]) and QoE metrics ([tracked]) a flow emits. Needs a [Context] only
 * because the manager creates one at construction; the track* helpers themselves never touch it.
 */
class RecordingAnalytics(context: Context) {

    val events = FakeAnalyticsProvider()
    val metrics = FakeMetricsProvider()
    val crashReporter = FakeCrashReporter()

    val manager = AnalyticsManager(
        context = context,
        analytics = events,
        metrics = metrics,
        crashReporter = crashReporter,
        settingsPreferences = FakeSettingsPreferences(),
        scope = CoroutineScope(Dispatchers.Unconfined),
        logger = FakeAppLogger(),
    )

    val captured: List<Pair<String, Map<String, Any>>> get() = events.capturedEvents
    val tracked: List<Pair<String, Map<String, Any?>>> get() = metrics.trackedEvents

    /** Properties of the most recent behavioral event named [name], or null if none was captured. */
    fun event(name: String): Map<String, Any>? =
        events.capturedEvents.lastOrNull { it.first == name }?.second

    /** Properties of the most recent QoE metric named [name], or null if none was tracked. */
    fun metric(name: String): Map<String, Any?>? =
        metrics.trackedEvents.lastOrNull { it.first == name }?.second

    fun eventNames(): List<String> = events.capturedEvents.map { it.first }
}
