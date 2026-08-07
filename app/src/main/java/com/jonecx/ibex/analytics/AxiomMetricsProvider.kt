package com.jonecx.ibex.analytics

import android.content.Context
import android.os.Build
import com.jonecx.ibex.BuildConfig
import com.jonecx.ibex.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Axiom backend for QoE metrics; a hand-rolled HTTP ingest (no vendor SDK) behind [MetricsProvider].
 * No-ops entirely when AXIOM_API_KEY is absent from local.properties, or while consent is off.
 */
class AxiomMetricsProvider(
    private val context: Context,
    private val logger: AppLogger,
) : MetricsProvider {

    private val consent = AtomicBoolean(false)
    private var enabled = false

    // Queue, formatter, and the queue file are all confined to this dispatcher; no locking needed.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val queue = ArrayDeque<JSONObject>()

    // Write-ahead queue on disk (excluded from cloud backup): survives crashes and force-stops.
    private var queueFile: File? = null
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun initialize() {
        if (BuildConfig.AXIOM_API_KEY.isBlank()) return
        queueFile = File(context.noBackupFilesDir, "axiom_queue.jsonl")
        scope.launch {
            restoreFromDisk()
            while (isActive) {
                delay(FLUSH_INTERVAL)
                drain()
            }
        }
        enabled = true
    }

    override fun setConsent(granted: Boolean) = consent.set(granted)

    override fun track(event: String, properties: Map<String, Any?>) {
        if (!enabled || !consent.get()) return
        val capturedAtMs = System.currentTimeMillis()
        scope.launch { enqueue(event, capturedAtMs, properties) }
    }

    override fun flush() {
        if (!enabled) return
        scope.launch { drain() }
    }

    private fun enqueue(eventName: String, capturedAtMs: Long, properties: Map<String, Any?>) {
        val row = JSONObject().apply {
            put("_time", timeFormat.format(Date(capturedAtMs)))
            put("event", eventName)
            put("app", "ibex")
            put("app_build_type", if (BuildConfig.DEBUG) "debug" else "release")
            put("app_version", BuildConfig.VERSION_NAME)
            put("os_version", Build.VERSION.RELEASE)
            put("api_level", Build.VERSION.SDK_INT)
            put("device_manufacturer", Build.MANUFACTURER)
            put("device_model", Build.MODEL)
            put("network_type", NetworkContext.type().wire)
            for ((key, value) in properties) value?.let { put(key, it) }
        }
        val evicting = queue.size >= MAX_QUEUE_SIZE
        if (evicting) queue.removeFirst()
        queue.addLast(row)
        // Disk first, memory second is the durability rule; eviction needs a full rewrite.
        if (evicting) rewriteDisk() else appendToDisk(row)
        if (queue.size >= FLUSH_AT) drain()
    }

    // Ships queued rows in batches; on failure the batch is requeued for the next interval.
    private fun drain() {
        // Offline uploads would just time out and requeue; wait for the next tick instead.
        if (NetworkContext.type() == NetworkContext.NetworkType.NONE) return
        while (queue.isNotEmpty()) {
            val batch = List(minOf(queue.size, MAX_BATCH_SIZE)) { queue.removeFirst() }
            if (!post(batch)) {
                batch.asReversed().forEach(queue::addFirst)
                return
            }
            // Batch confirmed; drop it from disk. A crash before this re-sends the batch next
            // launch: at-least-once delivery, acceptable for analytics.
            rewriteDisk()
        }
    }

    private fun restoreFromDisk() {
        val file = queueFile ?: return
        if (!file.exists()) return
        runCatching {
            file.forEachLine { line ->
                if (line.isNotBlank()) runCatching { queue.addLast(JSONObject(line)) }
            }
        }
    }

    private fun appendToDisk(row: JSONObject) {
        val file = queueFile ?: return
        runCatching { file.appendText(row.toString() + "\n") }
    }

    private fun rewriteDisk() {
        val file = queueFile ?: return
        runCatching {
            val tmp = File(file.parent, "${file.name}.tmp")
            tmp.bufferedWriter().use { writer ->
                queue.forEach { row ->
                    writer.write(row.toString())
                    writer.newLine()
                }
            }
            tmp.renameTo(file)
        }
    }

    private fun post(batch: List<JSONObject>): Boolean = runCatching {
        val url = URL("${BuildConfig.AXIOM_HOST}/v1/datasets/${BuildConfig.AXIOM_DATASET}/ingest")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.AXIOM_API_KEY}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.outputStream.use { it.write(JSONArray(batch).toString().toByteArray()) }
            val success = connection.responseCode in 200..299
            if (!success) logger.w("Axiom ingest failed: HTTP ${connection.responseCode}")
            success
        } finally {
            connection.disconnect()
        }
    }.getOrElse { error ->
        logger.w("Axiom ingest failed: ${error.javaClass.simpleName}")
        false
    }

    private companion object {
        const val FLUSH_AT = 20
        val FLUSH_INTERVAL = 30.seconds
        const val MAX_QUEUE_SIZE = 1000
        const val MAX_BATCH_SIZE = 50
        const val TIMEOUT_MS = 10_000
    }
}
