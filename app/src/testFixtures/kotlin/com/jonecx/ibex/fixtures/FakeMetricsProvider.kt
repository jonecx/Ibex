package com.jonecx.ibex.fixtures

import com.jonecx.ibex.analytics.MetricsProvider

class FakeMetricsProvider : MetricsProvider {

    val trackedEvents = mutableListOf<Pair<String, Map<String, Any?>>>()
    var initialized = false
    var consentGranted = false

    override fun initialize() {
        initialized = true
    }

    override fun track(event: String, properties: Map<String, Any?>) {
        trackedEvents.add(event to properties)
    }

    override fun setConsent(granted: Boolean) {
        consentGranted = granted
    }

    override fun flush() = Unit

    fun reset() {
        trackedEvents.clear()
        initialized = false
        consentGranted = false
    }
}
