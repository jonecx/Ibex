package com.jonecx.ibex.analytics

interface AnalyticsProvider {
    fun initialize()
    fun identify(userId: String)
    fun capture(event: String, properties: Map<String, Any> = emptyMap())
}
