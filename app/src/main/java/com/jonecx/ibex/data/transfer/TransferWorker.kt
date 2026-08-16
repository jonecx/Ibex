package com.jonecx.ibex.data.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.jonecx.ibex.MainActivity
import com.jonecx.ibex.R
import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.util.formatFileSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

// Long-running foreground worker that drains the transfer queue. It owns none of the transfer logic;
// it just keeps the process alive (foreground + wake/wifi locks) and mirrors progress to a notification.
class TransferWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val manager: TransferManager by inject()

    private companion object {
        const val CHANNEL_ID = "ibex_transfers"
        const val NOTIFICATION_ID = 4711
        const val WAKE_TAG = "ibex:transfer"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foregroundInfo(buildNotification(manager.snapshot.value))

    override suspend fun doWork(): Result {
        // A chained/duplicate worker can arrive after the queue is already drained; don't flash an
        // empty foreground service in that case.
        if (!manager.hasPendingWork()) return Result.success()

        ensureChannel()
        setForeground(getForegroundInfo())

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_TAG)
            .apply { setReferenceCounted(false) }
        val wifiLock = if (manager.hasRemoteWork()) {
            val wifiManager = applicationContext.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WAKE_TAG)
        } else {
            null
        }

        return try {
            wakeLock.acquire()
            wifiLock?.acquire()
            coroutineScope {
                // Mirror progress to the notification. The snapshot itself is throttled (~5/s), so this is cheap.
                val notifier = launch {
                    manager.snapshot.collect { snapshot ->
                        if (snapshot.hasActive) {
                            notificationManager().notify(NOTIFICATION_ID, buildNotification(snapshot))
                        }
                    }
                }
                manager.runQueue()
                notifier.cancel()
            }
            Result.success()
        } catch (e: CancellationException) {
            // Stopped by the system (or reboot). runJob already re-queued in-flight work for resume.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Transfer worker error")
            Result.success()
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
            if (wifiLock?.isHeld == true) wifiLock.release()
        }
    }

    private fun foregroundInfo(notification: android.app.Notification): ForegroundInfo =
        ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

    private fun buildNotification(snapshot: TransferSnapshot): android.app.Notification {
        val operation = snapshot.jobs.firstOrNull {
            it.status == TransferStatus.RUNNING || it.status == TransferStatus.QUEUED
        }?.operation
        val title = when (operation) {
            ClipboardOperation.MOVE -> applicationContext.getString(R.string.transfer_notification_moving)
            else -> applicationContext.getString(R.string.transfer_notification_copying)
        }
        val text = if (snapshot.totalBytes > 0L) {
            "${formatFileSize(snapshot.bytesDone)} / ${formatFileSize(snapshot.totalBytes)}"
        } else {
            applicationContext.getString(R.string.transfer_notification_preparing)
        }

        val openApp = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelAll = PendingIntent.getBroadcast(
            applicationContext,
            0,
            Intent(applicationContext, TransferActionReceiver::class.java)
                .setAction(TransferActionReceiver.ACTION_CANCEL_ALL),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_transfer_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(0, applicationContext.getString(R.string.transfer_cancel_all), cancelAll)

        if (snapshot.totalBytes > 0L) {
            builder.setProgress(100, (snapshot.fraction * 100).toInt(), false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.transfer_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
