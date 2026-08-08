package com.jonecx.ibex.analytics

import android.content.Context
import androidx.core.content.edit
import com.jonecx.azmaree.player.model.PlayerTelemetry
import com.jonecx.azmaree.player.telemetry.AzmareeAnalytics
import com.jonecx.azmaree.player.telemetry.AzmareeLogger
import com.jonecx.azmaree.player.telemetry.PlayerEvent
import com.jonecx.azmaree.player.telemetry.VideoPrivacy
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.data.model.NetworkProtocol
import com.jonecx.ibex.data.model.SortDirection
import com.jonecx.ibex.data.model.SortField
import com.jonecx.ibex.data.model.ThemeMode
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Vendor-agnostic telemetry facade: fans app + player events out to behavioral analytics (PostHog),
 * QoE metrics (Axiom), and crash breadcrumbs (Sentry), with one opt-in consent gate over all three.
 */
class AnalyticsManager(
    private val context: Context,
    private val analytics: AnalyticsProvider,
    private val metrics: MetricsProvider,
    private val crashReporter: CrashReporter,
    private val settingsPreferences: SettingsPreferencesContract,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {

    fun initialize() {
        analytics.initialize()
        metrics.initialize()
        crashReporter.initialize()
        // Crash reporting is independent of this opt-in; only behavioral analytics + QoE metrics gate.
        scope.launch {
            settingsPreferences.sendAnalyticsEnabled.collect { granted ->
                analytics.setConsent(granted)
                metrics.setConsent(granted)
            }
        }
        identifyUser()
    }

    fun onNetworkChanged() = analytics.onNetworkChanged()

    fun flush() {
        analytics.flush()
        metrics.flush()
    }

    private fun identifyUser() {
        val userId = getOrCreateUserId()
        analytics.identify(userId)
        logger.d("AnalyticsManager: User identified as $userId")
    }

    private fun getOrCreateUserId(): String {
        val prefs = context.getSharedPreferences("analytics", Context.MODE_PRIVATE)
        var userId = prefs.getString("user_id", null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit { putString("user_id", userId) }
        }
        return userId
    }

    // Behavioral event: PostHog + a Sentry breadcrumb for crash context.
    internal fun capture(event: String, properties: Map<String, Any?> = emptyMap()) {
        val scrubbed = TelemetryScrubber.scrub(properties)
        crashReporter.breadcrumb(category = "analytics", message = event, data = scrubbed)
        analytics.capture(event, scrubbed.filterValues { it != null }.mapValues { it.value!! })
        logger.d("AnalyticsManager: Sent $event")
    }

    // QoE metric: Axiom + a Sentry breadcrumb.
    internal fun trackMetric(event: String, properties: Map<String, Any?> = emptyMap()) {
        crashReporter.breadcrumb(category = "qoe", message = event, data = properties)
        metrics.track(event, properties)
    }

    private fun reportError(message: String, throwable: Throwable?) {
        crashReporter.recordException(throwable, message)
    }

    fun trackScreenView(screenName: String, properties: Map<String, Any> = emptyMap()) {
        val props = mutableMapOf<String, Any>("screen_name" to screenName)
        props.putAll(properties)
        analytics.screen(screenName)
        crashReporter.navigationBreadcrumb(screenName)
        capture("screen_view", props)
    }

    fun trackScreenExit(screenName: String, durationMs: Long) {
        capture(
            "screen_exit",
            mapOf(
                "screen_name" to screenName,
                "duration_ms" to durationMs,
                "duration_seconds" to (durationMs / 1000.0),
            ),
        )
    }

    fun trackTileClick(tileName: String, tileId: String, sourceType: FileSourceType? = null) {
        capture(
            "tile_click",
            props(
                "tile_name" to tileName,
                "tile_id" to tileId,
                "source_type" to sourceType?.wire(),
            ),
        )
    }

    // region App/behavioral/QoE event helpers
    //
    // Every app-side event flows through one of these typed helpers so this facade stays the single
    // fan-out point. Behavioral events go through capture() (PostHog + Sentry breadcrumb); QoE events
    // through trackMetric() (Axiom + breadcrumb). `network_type` is already a super-property on both
    // sinks, so it is never passed here. Anything carrying a path/url must be hashed (FileRef) or
    // scrubbed before it reaches these — the helpers below only ever take primitives and enums.

    // App start-up cost (Axiom QoE). App open/background funnels come from PostHog's built-in
    // lifecycle capture, so they are intentionally not re-emitted here.
    fun trackAppStart(durationMs: Long) = trackMetric("app_start", props("duration_ms" to durationMs))

    fun trackFileOpen(
        sourceType: FileSourceType,
        isRemote: Boolean,
        fileType: FileType,
        isDirectory: Boolean,
        sizeBytes: Long,
    ) = capture(
        "file_open",
        props(
            "source_type" to sourceType.wire(),
            "is_remote" to isRemote,
            "file_type" to fileType.wire(),
            "is_directory" to isDirectory,
            "size_bytes" to sizeBytes,
        ),
    )

    // Copy/move execute at paste time; the ClipboardOperation selects the event name. Remote transfers
    // additionally emit a QoE metric so throughput is queryable in Axiom.
    fun trackPaste(
        operation: ClipboardOperation,
        sourceType: FileSourceType,
        isRemote: Boolean,
        itemCount: Int,
        sizeBytes: Long,
        success: Boolean,
        durationMs: Long,
    ) {
        val event = when (operation) {
            ClipboardOperation.COPY -> "file_copy"
            ClipboardOperation.MOVE -> "file_move"
        }
        capture(
            event,
            props(
                "source_type" to sourceType.wire(),
                "is_remote" to isRemote,
                "item_count" to itemCount,
                "size_bytes" to sizeBytes,
                "result" to result(success),
                "duration_ms" to durationMs,
            ),
        )
        if (isRemote) {
            trackMetric(
                "file_transfer",
                props(
                    "source_type" to sourceType.wire(),
                    "item_count" to itemCount,
                    "size_bytes" to sizeBytes,
                    "duration_ms" to durationMs,
                    "result" to result(success),
                ),
            )
        }
    }

    fun trackFileDelete(
        sourceType: FileSourceType,
        isRemote: Boolean,
        itemCount: Int,
        permanent: Boolean,
        success: Boolean,
        durationMs: Long,
    ) = capture(
        "file_delete",
        props(
            "source_type" to sourceType.wire(),
            "is_remote" to isRemote,
            "item_count" to itemCount,
            "disposition" to if (permanent) "permanent" else "trash",
            "result" to result(success),
            "duration_ms" to durationMs,
        ),
    )

    fun trackFileRename(
        sourceType: FileSourceType,
        isRemote: Boolean,
        fileType: FileType,
        success: Boolean,
        durationMs: Long,
    ) = capture(
        "file_rename",
        props(
            "source_type" to sourceType.wire(),
            "is_remote" to isRemote,
            "file_type" to fileType.wire(),
            "result" to result(success),
            "duration_ms" to durationMs,
        ),
    )

    fun trackFolderCreate(
        sourceType: FileSourceType,
        isRemote: Boolean,
        success: Boolean,
        durationMs: Long,
    ) = capture(
        "folder_create",
        props(
            "source_type" to sourceType.wire(),
            "is_remote" to isRemote,
            "result" to result(success),
            "duration_ms" to durationMs,
        ),
    )

    // Directory-listing latency (Axiom QoE). Fires on every listing, success or failure.
    fun trackContentLoad(
        sourceType: FileSourceType,
        isRemote: Boolean,
        itemCount: Int,
        durationMs: Long,
        success: Boolean,
        errorCode: String? = null,
    ) = trackMetric(
        "content_load",
        props(
            "source_type" to sourceType.wire(),
            "is_remote" to isRemote,
            "item_count" to itemCount,
            "duration_ms" to durationMs,
            "result" to result(success),
            "error_code" to errorCode,
        ),
    )

    fun trackContentEmpty(sourceType: FileSourceType, isRemote: Boolean, context: String) = capture(
        "content_empty",
        props("source_type" to sourceType.wire(), "is_remote" to isRemote, "context" to context),
    )

    fun trackContentError(sourceType: FileSourceType, isRemote: Boolean, errorCode: String) = capture(
        "content_error",
        props("source_type" to sourceType.wire(), "is_remote" to isRemote, "error_code" to errorCode),
    )

    fun trackConnectionAdded(protocol: NetworkProtocol, anonymous: Boolean) = capture(
        "connection_add",
        props("protocol" to protocol.wire(), "anonymous" to anonymous),
    )

    fun trackConnectionEdited(protocol: NetworkProtocol, anonymous: Boolean) = capture(
        "connection_edit",
        props("protocol" to protocol.wire(), "anonymous" to anonymous),
    )

    fun trackConnectionDeleted(protocol: NetworkProtocol?) = capture(
        "connection_delete",
        props("protocol" to protocol?.wire()),
    )

    // First reachability of a remote source. Emits the behavioral outcome and the connect latency.
    fun trackConnectionConnect(
        protocol: NetworkProtocol,
        success: Boolean,
        durationMs: Long,
        errorCode: String? = null,
    ) {
        capture(
            "connection_connect",
            props("protocol" to protocol.wire(), "result" to result(success), "error_code" to errorCode),
        )
        trackMetric(
            "connection_latency",
            props("protocol" to protocol.wire(), "result" to result(success), "duration_ms" to durationMs),
        )
    }

    fun trackThemeChange(from: ThemeMode, to: ThemeMode) =
        capture("theme_change", props("from" to from.wire(), "to" to to.wire()))

    fun trackViewModeChange(from: ViewMode, to: ViewMode) =
        capture("view_mode_change", props("from" to from.wire(), "to" to to.wire()))

    fun trackGridColumnsChange(from: Int, to: Int) =
        capture("grid_columns_change", props("from" to from, "to" to to))

    fun trackSortChange(field: SortField, direction: SortDirection) =
        capture("sort_change", props("field" to field.wire(), "direction" to direction.wire()))

    fun trackAnalyticsConsentChange(granted: Boolean) =
        capture("analytics_consent_change", props("granted" to granted))

    fun trackSearchStart(sourceType: FileSourceType) =
        capture("search_start", props("source_type" to sourceType.wire()))

    fun trackSearchPerform(sourceType: FileSourceType, queryLength: Int, resultCount: Int) = capture(
        "search_perform",
        props("source_type" to sourceType.wire(), "query_length" to queryLength, "result_count" to resultCount),
    )

    fun trackSearchClear(sourceType: FileSourceType) =
        capture("search_clear", props("source_type" to sourceType.wire()))

    fun trackStorageAnalysisStart(isRetry: Boolean) =
        capture("storage_analysis_start", props("trigger" to if (isRetry) "retry" else "initial"))

    fun trackStorageAnalysisComplete(
        success: Boolean,
        durationMs: Long,
        usedBytes: Long? = null,
        totalBytes: Long? = null,
        categoryCount: Int? = null,
        errorCode: String? = null,
    ) {
        capture(
            "storage_analysis_complete",
            props(
                "result" to result(success),
                "duration_ms" to durationMs,
                "used_bytes" to usedBytes,
                "total_bytes" to totalBytes,
                "category_count" to categoryCount,
                "error_code" to errorCode,
            ),
        )
        trackMetric("storage_analysis", props("result" to result(success), "duration_ms" to durationMs))
    }

    fun trackPermissionRequest(permissionType: String, sdkInt: Int) =
        capture("permission_request", props("permission_type" to permissionType, "sdk_int" to sdkInt))

    fun trackPermissionResult(permissionType: String, granted: Boolean, sdkInt: Int) = capture(
        "permission_result",
        props("permission_type" to permissionType, "granted" to granted, "sdk_int" to sdkInt),
    )

    fun trackMediaViewerOpen(itemCount: Int, mediaType: FileType, isRemote: Boolean, initialIndex: Int) = capture(
        "media_viewer_open",
        props(
            "item_count" to itemCount,
            "media_type" to mediaType.wire(),
            "is_remote" to isRemote,
            "initial_index" to initialIndex,
        ),
    )

    fun trackMediaViewerPage(mediaType: FileType, forward: Boolean, pageIndex: Int) = capture(
        "media_viewer_page",
        props("media_type" to mediaType.wire(), "direction" to if (forward) "next" else "prev", "page_index" to pageIndex),
    )

    fun trackMediaViewerClose(durationMs: Long, pagesViewed: Int) =
        capture("media_viewer_close", props("duration_ms" to durationMs, "pages_viewed" to pagesViewed))

    private fun result(success: Boolean): String = if (success) "success" else "failure"

    private fun Enum<*>.wire(): String = name.lowercase()

    // Assembles a property map, dropping null-valued keys so optional props simply vanish when absent.
    private fun props(vararg pairs: Pair<String, Any?>): Map<String, Any?> =
        pairs.toMap().filterValues { it != null }

    // endregion

    // Routes a log record (from AnalyticsTree) to the right sinks: warnings/errors become
    // behavioral events; errors are also captured by the crash reporter.
    internal fun trackLog(isError: Boolean, properties: Map<String, Any>, message: String, throwable: Throwable?) {
        capture(if (isError) "log_error" else "log_warning", properties)
        if (isError) reportError(message, throwable)
    }

    // The sink handed to every embedded AzmareePlayer: QoE -> Axiom, behavior -> PostHog, every
    // event and log -> Sentry breadcrumbs. Video urls arrive hashed (VideoPrivacy.HASHED).
    val playerTelemetry: PlayerTelemetry by lazy {
        PlayerTelemetry(
            analytics = AzmareeAnalytics { event ->
                when (event.telemetryType) {
                    PlayerEvent.TelemetryType.QOE -> trackMetric(event.eventName, event.properties())
                    PlayerEvent.TelemetryType.BEHAVIOR -> capture(event.eventName, event.properties())
                }
            },
            logger = object : AzmareeLogger {
                override fun log(level: AzmareeLogger.Level, message: String, throwable: Throwable?) {
                    when (level) {
                        AzmareeLogger.Level.DEBUG -> logger.d(message)
                        AzmareeLogger.Level.INFO -> logger.i(message)
                        AzmareeLogger.Level.WARN -> logger.w(message)
                        AzmareeLogger.Level.ERROR -> logger.e(message, throwable)
                    }
                    if (level == AzmareeLogger.Level.ERROR) reportError(message, throwable)
                }
            },
            videoPrivacy = VideoPrivacy.HASHED,
        )
    }
}
