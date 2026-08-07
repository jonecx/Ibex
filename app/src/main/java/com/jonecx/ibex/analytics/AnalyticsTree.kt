package com.jonecx.ibex.analytics

import android.util.Log
import timber.log.Timber

open class AnalyticsTree(
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

        // The facade scrubs remote urls out of these properties before they leave the device.
        analyticsManager.trackLog(
            isError = priority == Log.ERROR,
            properties = properties,
            message = message,
            throwable = t,
        )
    }
}
