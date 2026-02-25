package com.jonecx.ibex.fixtures

import com.jonecx.ibex.analytics.AnalyticsProvider

class FakeAnalyticsProvider : AnalyticsProvider {

    val capturedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    var identifiedUserId: String? = null
    var initialized = false

    override fun initialize() {
        initialized = true
    }

    override fun identify(userId: String) {
        identifiedUserId = userId
    }

    override fun capture(event: String, properties: Map<String, Any>) {
        capturedEvents.add(event to properties)
    }

    fun reset() {
        capturedEvents.clear()
        identifiedUserId = null
        initialized = false
    }
}
