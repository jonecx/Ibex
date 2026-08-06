package com.jonecx.ibex.logging

import com.jonecx.ibex.analytics.AnalyticsTree
import timber.log.Timber

class TimberLogger(
    private val analyticsTree: () -> AnalyticsTree,
) : AppLogger {

    override fun initialize() {
        Timber.plant(analyticsTree())
    }

    override fun d(message: String) = Timber.d(message)

    override fun i(message: String) = Timber.i(message)

    override fun w(message: String) = Timber.w(message)

    override fun e(message: String, throwable: Throwable?) {
        if (throwable != null) {
            Timber.e(throwable, message)
        } else {
            Timber.e(message)
        }
    }
}
