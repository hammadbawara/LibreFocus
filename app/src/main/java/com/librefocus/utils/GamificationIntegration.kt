package com.librefocus.utils

import android.util.Log
import com.librefocus.data.repository.GamificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility for integrating gamification with usage tracking.
 * Syncs gamification data whenever usage stats are updated.
 */
object GamificationIntegration {
    private const val TAG = "GamificationIntegration"

    /**
     * Performs daily gamification sync.
     * Called from UsageSyncWorker after usage stats are synced.
     */
    suspend fun syncDailyGamification(
        gamificationRepository: GamificationRepository
    ) = withContext(Dispatchers.IO) {
        try {
            gamificationRepository.syncDailyGamification()
            Log.d(TAG, "Daily gamification sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing gamification", e)
        }
    }

    /**
     * Initializes gamification system on first run.
     */
    suspend fun initializeGamification(
        gamificationRepository: GamificationRepository
    ) = withContext(Dispatchers.IO) {
        try {
            gamificationRepository.initializeBadges()
            gamificationRepository.generateTodayChallenge()
            gamificationRepository.initializeLocalLeaderboard()
            Log.d(TAG, "Gamification initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing gamification", e)
        }
    }
}

