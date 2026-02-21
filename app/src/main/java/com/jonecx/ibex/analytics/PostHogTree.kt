package com.jonecx.ibex.analytics

import android.util.Log
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.posthog.PostHog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class PostHogTree(
    private val settingsPreferences: SettingsPreferencesContract,
    private val scope: CoroutineScope,
) : Timber.Tree() {

    private val isAnalyticsEnabled = AtomicBoolean(false)

    init {
        scope.launch {
            settingsPreferences.sendAnalyticsEnabled.collect { enabled ->
                isAnalyticsEnabled.set(enabled)
            }
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority != Log.ERROR && priority != Log.WARN) return
        if (!isAnalyticsEnabled.get()) return

        val properties = mutableMapOf<String, Any>(
            "tag" to (tag ?: "unknown"),
            "message" to message,
        )

        t?.let {
            properties["exception"] = it.javaClass.simpleName
            properties["stacktrace"] = it.stackTraceToString().take(1000)
        }

        val event = if (priority == Log.ERROR) "log_error" else "log_warning"
        PostHog.capture(event, null, properties)
    }
}
