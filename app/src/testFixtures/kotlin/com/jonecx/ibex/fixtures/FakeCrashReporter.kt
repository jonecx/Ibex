package com.jonecx.ibex.fixtures

import com.jonecx.ibex.analytics.CrashReporter

class FakeCrashReporter : CrashReporter {

    val breadcrumbs = mutableListOf<Pair<String, String>>()
    val recordedExceptions = mutableListOf<Pair<String, Throwable?>>()
    val navigations = mutableListOf<String>()
    var initialized = false

    override fun initialize() {
        initialized = true
    }

    override fun breadcrumb(category: String, message: String, isError: Boolean, data: Map<String, Any?>) {
        breadcrumbs.add(category to message)
    }

    override fun recordException(throwable: Throwable?, message: String) {
        recordedExceptions.add(message to throwable)
    }

    override fun navigationBreadcrumb(route: String) {
        navigations.add(route)
    }

    fun reset() {
        breadcrumbs.clear()
        recordedExceptions.clear()
        navigations.clear()
        initialized = false
    }
}
