package com.jonecx.ibex.analytics

import android.util.Log
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.posthog.PostHog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class PostHogTree(
    private val settingsPreferences: SettingsPreferencesContract,
) : Timber.Tree() {

    private fun isAnalyticsEnabled(): Boolean {
        return runBlocking { settingsPreferences.sendAnalyticsEnabled.first() }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority != Log.ERROR && priority != Log.WARN) return
        if (!isAnalyticsEnabled()) return

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
