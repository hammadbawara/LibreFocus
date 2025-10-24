package com.librefocus.utils

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.librefocus.workers.UsageSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Utility class for scheduling usage sync operations.
 */
object UsageSyncScheduler {

    /**
     * Schedules periodic usage sync every hour.
     * Uses WorkManager to ensure sync runs even when app is closed.
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Can run without network
            .setRequiresBatteryNotLow(false) // Can run on low battery
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<UsageSyncWorker>(
            repeatInterval = 1, // Every 1 hour
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                5, // Initial backoff delay
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            syncWorkRequest
        )
    }

    /**
     * Schedules a one-time sync operation immediately.
     */
    fun scheduleOneTimeSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<UsageSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncWorkRequest)
    }

    /**
     * Cancels all scheduled sync operations.
     */
    fun cancelAllSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC_SYNC)
    }

    /**
     * Gets the current status of periodic sync work.
     */
    fun getPeriodicSyncStatus(context: Context) =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(WORK_NAME_PERIODIC_SYNC)

    private const val WORK_NAME_PERIODIC_SYNC = "usage_periodic_sync"
}