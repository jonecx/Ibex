package com.jonecx.ibex.data.transfer

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

// Starts the foreground worker that drains the queue. Behind an interface so the manager stays free of
// WorkManager types and stays unit-testable with a fake.
interface TransferScheduler {
    fun ensureRunning()
}

class WorkManagerTransferScheduler(private val context: Context) : TransferScheduler {

    override fun ensureRunning() {
        val request = OneTimeWorkRequestBuilder<TransferWorker>().build()
        // APPEND_OR_REPLACE keeps runs strictly sequential; a new paste is drained by the current
        // worker if it is still going, or by this appended one if it just finished.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "ibex-transfers"
    }
}
