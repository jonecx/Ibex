package com.jonecx.ibex.analytics

// Quality-of-experience metrics sink (Axiom). Vendor-agnostic so the backend stays swappable.
interface MetricsProvider {
    fun initialize()
    fun track(event: String, properties: Map<String, Any?> = emptyMap())
    fun setConsent(granted: Boolean)
    fun flush()
}
