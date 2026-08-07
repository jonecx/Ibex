package com.jonecx.ibex.analytics

// Behavioral analytics sink (PostHog). Vendor-agnostic so the backend stays swappable.
interface AnalyticsProvider {
    fun initialize()
    fun identify(userId: String)
    fun capture(event: String, properties: Map<String, Any> = emptyMap())
    fun screen(name: String)
    fun setConsent(granted: Boolean)
    fun onNetworkChanged()
    fun flush()
}
