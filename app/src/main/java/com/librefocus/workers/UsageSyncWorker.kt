package com.librefocus.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.librefocus.data.repository.UsageTrackingRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * WorkManager worker for periodic usage stats synchronization.
 * Runs in background and syncs usage data automatically.
 */
class UsageSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val repository: UsageTrackingRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting background usage sync")

            // Perform incremental sync (only new data since last sync)
            repository.syncUsageStats()

            Log.d(TAG, "Background usage sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background usage sync failed", e)
            Result.retry() // Retry on failure
        }
    }

    companion object {
        private const val TAG = "UsageSyncWorker"
    }
}