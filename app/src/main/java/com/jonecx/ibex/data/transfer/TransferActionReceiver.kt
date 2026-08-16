package com.jonecx.ibex.data.transfer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// Backs the transfer notification's "Cancel all" action. Manifest-registered and not exported, so the
// notification's PendingIntent can reach it even after the app UI is gone. Cancelling drains the queue and
// the foreground worker then finishes on its own, clearing the notification.
class TransferActionReceiver : BroadcastReceiver(), KoinComponent {

    private val manager: TransferManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CANCEL_ALL) manager.cancelAll()
    }

    companion object {
        const val ACTION_CANCEL_ALL = "com.jonecx.ibex.action.TRANSFER_CANCEL_ALL"
    }
}
