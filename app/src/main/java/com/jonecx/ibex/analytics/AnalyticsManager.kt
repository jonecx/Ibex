package com.jonecx.ibex.analytics

import android.content.Context
import androidx.core.content.edit
import com.jonecx.ibex.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsProvider: AnalyticsProvider,
    private val logger: AppLogger,
) {

    fun initialize() {
        analyticsProvider.initialize()
        identifyUser()
    }

    private fun identifyUser() {
        val userId = getOrCreateUserId()
        analyticsProvider.identify(userId)
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

    internal fun capture(event: String, properties: Map<String, Any> = emptyMap()) {
        analyticsProvider.capture(event, properties)
        logger.d("AnalyticsManager: Sent $event")
    }

    fun trackScreenView(screenName: String, properties: Map<String, Any> = emptyMap()) {
        val props = mutableMapOf<String, Any>("screen_name" to screenName)
        props.putAll(properties)
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

    fun trackTileClick(tileName: String, tileId: String) {
        capture(
            "tile_click",
            mapOf(
                "tile_name" to tileName,
                "tile_id" to tileId,
            ),
        )
    }
}
