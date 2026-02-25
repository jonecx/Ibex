package com.jonecx.ibex.analytics

import android.util.Log
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AnalyticsTree @Inject constructor(
    private val analyticsManager: AnalyticsManager,
) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority != Log.ERROR && priority != Log.WARN) return

        val properties = mutableMapOf<String, Any>(
            "tag" to (tag ?: "unknown"),
            "message" to message,
        )

        t?.let {
            properties["exception"] = it.javaClass.simpleName
            properties["stacktrace"] = it.stackTraceToString().take(1000)
        }

        val event = if (priority == Log.ERROR) "log_error" else "log_warning"
        analyticsManager.capture(event, properties)
    }
}
