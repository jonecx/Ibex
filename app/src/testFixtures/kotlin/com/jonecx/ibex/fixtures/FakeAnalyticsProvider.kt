package com.jonecx.ibex.fixtures

import com.jonecx.ibex.analytics.AnalyticsProvider

class FakeAnalyticsProvider : AnalyticsProvider {

    val capturedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    val screens = mutableListOf<String>()
    var identifiedUserId: String? = null
    var initialized = false
    var consentGranted = false

    override fun initialize() {
        initialized = true
    }

    override fun identify(userId: String) {
        identifiedUserId = userId
    }

    override fun capture(event: String, properties: Map<String, Any>) {
        capturedEvents.add(event to properties)
    }

    override fun screen(name: String) {
        screens.add(name)
    }

    override fun setConsent(granted: Boolean) {
        consentGranted = granted
    }

    override fun onNetworkChanged() = Unit

    override fun flush() = Unit

    fun reset() {
        capturedEvents.clear()
        screens.clear()
        identifiedUserId = null
        initialized = false
        consentGranted = false
    }
}
