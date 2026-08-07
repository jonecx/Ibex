package com.jonecx.ibex.analytics

// Crash + error reporting sink (Sentry). Events/logs become breadcrumbs so a crash arrives with
// the trail that led to it. Vendor-agnostic so the backend stays swappable. Independent of the
// analytics opt-in: crashes are always reported (anonymized, no PII).
interface CrashReporter {
    fun initialize()
    fun breadcrumb(category: String, message: String, isError: Boolean = false, data: Map<String, Any?> = emptyMap())
    fun recordException(throwable: Throwable?, message: String)
    fun navigationBreadcrumb(route: String)
}
