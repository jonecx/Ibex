package com.jonecx.ibex.analytics

import android.content.Context
import com.jonecx.ibex.BuildConfig
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid

/**
 * Sentry backend for crash reporting behind [CrashReporter]. No-ops entirely when SENTRY_DSN is
 * absent. Independent of the analytics opt-in: crashes are always reported, anonymized
 * (isSendDefaultPii = false) with remote urls scrubbed out.
 */
class SentryCrashReporter(
    private val context: Context,
) : CrashReporter {

    private var enabled = false

    override fun initialize() {
        val dsn = BuildConfig.SENTRY_DSN
        if (dsn.isBlank()) return
        SentryAndroid.init(context) { options ->
            options.dsn = dsn
            options.environment = if (BuildConfig.DEBUG) "debug" else "release"
            options.isDebug = BuildConfig.DEBUG
            options.isAttachStacktrace = true
            // Crashes + logs only; tracing stays off to fit the free error budget.
            options.tracesSampleRate = 0.0
            // Never ship IP/device identifiers; remote urls are scrubbed before they reach here.
            options.isSendDefaultPii = false
            options.setTag("app", "ibex")
            options.setTag("app_build_type", if (BuildConfig.DEBUG) "debug" else "release")
            // Scrub remote urls/credentials out of exception messages and breadcrumbs (incl. the
            // SDK's auto-captured ones) before they leave the device.
            options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                event.message?.apply {
                    formatted = formatted?.let(TelemetryScrubber::scrub)
                    message = message?.let(TelemetryScrubber::scrub)
                }
                event.exceptions?.forEach { it.value = it.value?.let(TelemetryScrubber::scrub) }
                event
            }
            options.beforeBreadcrumb = SentryOptions.BeforeBreadcrumbCallback { breadcrumb, _ ->
                breadcrumb.message = breadcrumb.message?.let(TelemetryScrubber::scrub)
                breadcrumb
            }
        }
        enabled = true
    }

    override fun breadcrumb(category: String, message: String, isError: Boolean, data: Map<String, Any?>) {
        if (!enabled) return
        Sentry.addBreadcrumb(
            Breadcrumb().apply {
                this.category = category
                this.message = TelemetryScrubber.scrub(message)
                this.level = if (isError) SentryLevel.ERROR else SentryLevel.INFO
                for ((key, value) in TelemetryScrubber.scrub(data)) value?.let { setData(key, it) }
            },
        )
    }

    override fun recordException(throwable: Throwable?, message: String) {
        if (!enabled) return
        if (throwable != null) {
            Sentry.captureException(throwable)
        } else {
            Sentry.captureMessage(TelemetryScrubber.scrub(message), SentryLevel.ERROR)
        }
    }

    override fun navigationBreadcrumb(route: String) {
        if (!enabled) return
        Sentry.addBreadcrumb(
            Breadcrumb().apply {
                type = "navigation"
                category = "navigation"
                this.message = route
                level = SentryLevel.INFO
            },
        )
    }
}
