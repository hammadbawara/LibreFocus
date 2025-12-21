package com.librefocus.data.repository

import android.util.Log
import com.librefocus.data.local.UsageStatsProvider
import com.librefocus.data.local.database.dao.AppCategoryDao
import com.librefocus.data.local.database.dao.AppDao
import com.librefocus.data.local.database.dao.DailyDeviceUsageDao
import com.librefocus.data.local.database.dao.HourlyAppUsageDao
import com.librefocus.data.local.database.dao.SyncMetadataDao
import com.librefocus.data.local.database.entity.AppCategoryEntity
import com.librefocus.data.local.database.entity.AppEntity
import com.librefocus.data.local.database.entity.DailyDeviceUsageEntity
import com.librefocus.data.local.database.entity.HourlyAppUsageEntity
import com.librefocus.data.local.database.entity.SyncMetadataEntity
import com.librefocus.models.AppUsageData
import com.librefocus.models.HourlyUsageData
import com.librefocus.models.UsageValuePoint
import com.librefocus.utils.roundToDayStart
import com.librefocus.utils.roundToHourStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Repository for managing app usage tracking.
 * Handles fetching usage stats from the system, aggregating by hour, and storing in the database.
 */
class UsageTrackingRepository(
    private val usageStatsProvider: UsageStatsProvider,
    private val appCategoryDao: AppCategoryDao,
    private val appDao: AppDao,
    private val hourlyAppUsageDao: HourlyAppUsageDao,
    private val dailyDeviceUsageDao: DailyDeviceUsageDao,
    private val syncMetadataDao: SyncMetadataDao
) {

    companion object {
        private const val TAG = "UsageTrackingRepository"
        private const val LAST_SYNC_KEY = "last_sync_timestamp_utc"
        private const val DEFAULT_CATEGORY_NAME = "Uncategorized"
    }

    suspend fun syncUsageStats() = withContext(Dispatchers.IO) {
        try{
            val lastTimeSyncDayStartUtc: Long = getLastSyncTime()?.let {
                roundToDayStart(it)
            } ?: 0L
            val currentUtc = System.currentTimeMillis()

            ensureDefaultCategoryExists()

            val hourlyUsageMap = usageStatsProvider.getHourlyUsageStatistics(lastTimeSyncDayStartUtc, currentUtc)
            saveHourlyUsageData(hourlyUsageMap)

            updateLastSyncTime(currentUtc)

            Log.d(TAG, "Sync completed successfully. Processed ${hourlyUsageMap.size} data")
    } catch (e: Exception) {
        Log.e(TAG, "Error syncing usage stats", e)
        throw e
    }
    }

    private suspend fun saveHourlyUsageData(
        hourlyUsageMap: Map<Pair<String, Long>, Pair<Long, Int>>
    ) {
        val defaultCategory = appCategoryDao.getCategoryByName(DEFAULT_CATEGORY_NAME)
            ?: throw IllegalStateException("Default category not found")

        hourlyUsageMap.forEach { (key, value) ->
            val (packageName, hourStartUtc) = key
            val (usageDuration, launchCount) = value

            // Ensure app exists in database
            var app = appDao.getAppByPackageName(packageName)
            if (app == null) {
                val appName = usageStatsProvider.getAppName(packageName)
                val appId = appDao.insertApp(
                    AppEntity(
                        packageName = packageName,
                        appName = appName,
                        categoryId = defaultCategory.id
                    )
                )
                app = appDao.getAppById(appId.toInt())
            }

            if (app != null) {
                hourlyAppUsageDao.insertUsage(
                    HourlyAppUsageEntity(
                        appId = app.id,
                        hourStartUtc = hourStartUtc,
                        usageDurationMillis = usageDuration,
                        launchCount = launchCount
                    )
                )
            }
        }
    }

    /**
     * Retrieves the last sync timestamp from the database.
     */
    private suspend fun getLastSyncTime(): Long? {
        return syncMetadataDao.getMetadata(LAST_SYNC_KEY)?.valueUtc
    }

    /**
     * Updates the last sync timestamp in the database.
     */
    private suspend fun updateLastSyncTime(timestampUtc: Long) {
        syncMetadataDao.insertMetadata(
            SyncMetadataEntity(
                key = LAST_SYNC_KEY,
                valueUtc = timestampUtc
            )
        )
    }

    /**
     * Ensures the default category exists in the database.
     */
    private suspend fun ensureDefaultCategoryExists() {
        val existing = appCategoryDao.getCategoryByName(DEFAULT_CATEGORY_NAME)
        if (existing == null) {
            appCategoryDao.insertCategory(
                AppCategoryEntity(
                    categoryName = DEFAULT_CATEGORY_NAME,
                    isCustom = false,
                    addedAtUtc = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Retrieves hourly usage data for a specific time range.
     */
    fun getHourlyUsageInTimeRange(
        startUtc: Long,
        endUtc: Long
    ): Flow<List<HourlyUsageData>> {
        return hourlyAppUsageDao.getUsageInTimeRange(startUtc, endUtc).map { usageEntities ->
            usageEntities.mapNotNull { usage ->
                val app = appDao.getAppById(usage.appId)
                app?.let {
                    HourlyUsageData(
                        packageName = it.packageName,
                        appName = it.appName,
                        hourStartUtc = usage.hourStartUtc,
                        usageDurationMillis = usage.usageDurationMillis,
                        launchCount = usage.launchCount
                    )
                }
            }
        }
    }

    /**
     * Retrieves daily unlock summaries for the provided UTC range (inclusive of start day, exclusive of end day).
     */
    suspend fun getDailyUnlockSummary(startUtc: Long, endUtc: Long): List<DailyDeviceUsageEntity> {
        val rangeStart = roundToDayStart(startUtc)
        val rangeEndExclusive = roundToDayStart(endUtc) + TimeUnit.DAYS.toMillis(1)
        return dailyDeviceUsageDao.getUsageForDayRange(rangeStart, rangeEndExclusive)
    }

    /**
     * Retrieves aggregated usage per app for a time range.
     */
    suspend fun getAppUsageSummaryInTimeRange(
        startUtc: Long,
        endUtc: Long
    ): List<AppUsageData> = withContext(Dispatchers.IO) {
        val usageMap = mutableMapOf<Int, Pair<Long, Int>>()
        val usages = hourlyAppUsageDao.getUsageInTimeRangeOnce(startUtc, endUtc)
        usages.forEach { usage ->
            val current = usageMap[usage.appId] ?: (0L to 0)
            usageMap[usage.appId] = (
                current.first + usage.usageDurationMillis to
                current.second + usage.launchCount
            )
        }

        usageMap.mapNotNull { (appId, data) ->
            val app = appDao.getAppById(appId)
            app?.let {
                AppUsageData(
                    packageName = it.packageName,
                    appName = it.appName,
                    usageDurationMillis = data.first,
                    launchCount = data.second
                )
            }
        }.sortedByDescending { it.usageDurationMillis }
    }

    /**
     * Deletes old usage data before a specified date.
     */
    suspend fun cleanupOldUsageData(beforeUtc: Long): Int {
        return hourlyAppUsageDao.deleteUsagesBefore(beforeUtc)
    }

    /**
     * Aggregates total usage duration and launches grouped by hour for the supplied range.
     */
    suspend fun getUsageTotalsGroupedByHour(
        startUtc: Long,
        endUtc: Long
    ): List<UsageValuePoint> = withContext(Dispatchers.IO) {
        val buckets = mutableMapOf<Long, Pair<Long, Int>>()
        val usageEntries = hourlyAppUsageDao.getUsageInTimeRangeOnce(startUtc, endUtc)
        usageEntries.forEach { entry ->
            val bucketKey = roundToHourStart(entry.hourStartUtc)
            val current = buckets[bucketKey] ?: (0L to 0)
            buckets[bucketKey] = (
                current.first + entry.usageDurationMillis to
                current.second + entry.launchCount
            )
        }
        buckets.entries
            .sortedBy { it.key }
            .map { (bucketStart, totals) ->
                UsageValuePoint(
                    bucketStartUtc = bucketStart,
                    totalUsageMillis = totals.first,
                    totalLaunchCount = totals.second
                )
            }
    }

    /**
     * Aggregates total usage duration and launches grouped by day for the supplied range.
     */
    suspend fun getUsageTotalsGroupedByDay(
        startUtc: Long,
        endUtc: Long
    ): List<UsageValuePoint> = withContext(Dispatchers.IO) {
        val buckets = mutableMapOf<Long, Pair<Long, Int>>()
        val usageEntries = hourlyAppUsageDao.getUsageInTimeRangeOnce(startUtc, endUtc)
        usageEntries.forEach { entry ->
            val bucketKey = roundToDayStart(entry.hourStartUtc)
            if (bucketKey >= startUtc && bucketKey < endUtc) {
                val current = buckets[bucketKey] ?: (0L to 0)
                buckets[bucketKey] = (
                    current.first + entry.usageDurationMillis to
                    current.second + entry.launchCount
                )
            }
        }
        buckets.entries
            .sortedBy { it.key }
            .map { (bucketStart, totals) ->
                UsageValuePoint(
                    bucketStartUtc = bucketStart,
                    totalUsageMillis = totals.first,
                    totalLaunchCount = totals.second
                )
            }
    }
}
