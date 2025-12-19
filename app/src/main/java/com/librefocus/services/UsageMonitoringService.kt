package com.librefocus.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.librefocus.data.repository.UsageTrackingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Foreground service for continuous usage monitoring.
 * Runs sync operations periodically while the service is active.
 */
class UsageMonitoringService : Service() {

    private val repository: UsageTrackingRepository by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Usage monitoring service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Usage monitoring service started")

        // Start periodic sync
        startPeriodicSync()

        // Return START_STICKY to restart service if killed
        return START_STICKY
    }

    private fun startPeriodicSync() {
        syncJob?.cancel()

        syncJob = serviceScope.launch {
            while (isActive) {
                try {
                    Log.d(TAG, "Running periodic usage sync")
                    repository.syncUsageStats()

                    // Wait 1 hour before next sync
                    delay(SYNC_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Periodic sync failed", e)
                    // Wait 5 minutes before retrying on error
                    delay(ERROR_RETRY_DELAY_MS)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Usage monitoring service destroyed")

        syncJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "UsageMonitoringService"
        private const val SYNC_INTERVAL_MS = 60 * 60 * 1000L // 1 hour
        private const val ERROR_RETRY_DELAY_MS = 5 * 60 * 1000L // 5 minutes
    }
}