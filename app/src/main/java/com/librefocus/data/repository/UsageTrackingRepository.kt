package com.librefocus.data.repository

import android.app.usage.UsageEvents
import android.util.Log
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
import com.librefocus.data.local.datasource.UsageStatsDataSource
import com.librefocus.models.AppUsageAverages
import com.librefocus.models.AppUsageData
import com.librefocus.models.HourlyUsageData
import com.librefocus.utils.extractUtcHourOfDay
import com.librefocus.utils.roundToDayStart
import com.librefocus.utils.roundToHourStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Repository for managing app usage tracking.
 * Handles fetching usage stats from the system, aggregating by hour, and storing in the database.
 */
class UsageTrackingRepository(
    private val usageStatsDataSource: UsageStatsDataSource,
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
    
    /**
     * Synchronizes usage stats from the system to the database.
     * Fetches new stats since the last sync and aggregates them hourly.
     * 
     * @param forceFullSync If true, ignores last sync time and fetches all available data
     */
    suspend fun syncUsageStats(forceFullSync: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val endTimeUtc = System.currentTimeMillis()
            val startTimeUtc = if (forceFullSync) {
                // Fetch last 30 days on full sync
                endTimeUtc - TimeUnit.DAYS.toMillis(30)
            } else {
                getLastSyncTime() ?: (endTimeUtc - TimeUnit.DAYS.toMillis(7))
            }
            
            Log.d(TAG, "Syncing usage stats from $startTimeUtc to $endTimeUtc")
            
            // Ensure default category exists
            ensureDefaultCategoryExists()
            
            // Fetch usage events from system
            val events = usageStatsDataSource.fetchUsageEvents(startTimeUtc, endTimeUtc)
            
            // Process events and aggregate by hour
            val hourlyUsageMap = aggregateUsageByHour(events)

            // Aggregate unlock information by day/hour
            val dailyUnlockMap = aggregateDailyUnlocks(events)
            
            // Save aggregated data to database
            saveHourlyUsageData(hourlyUsageMap)
            saveDailyUnlockData(dailyUnlockMap, endTimeUtc)
            
            // Update last sync time
            updateLastSyncTime(endTimeUtc)
            
            Log.d(TAG, "Sync completed successfully. Processed ${events.size} events")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing usage stats", e)
            throw e
        }
    }
    
    /**
     * Aggregates usage events by hour for each app.
     * Calculates duration between RESUMED and PAUSED/STOPPED events.
     * 
     * @param events List of usage events
     * @return Map of (packageName, hourStartUtc) to aggregated usage data
     */
    private fun aggregateUsageByHour(
        events: List<com.librefocus.models.UsageEventData>
    ): Map<Pair<String, Long>, Pair<Long, Int>> {
        val hourlyUsageMap = mutableMapOf<Pair<String, Long>, Long>()
        val activeSessionsMap = mutableMapOf<String, Long>()
        val launchCountMap = mutableMapOf<Pair<String, Long>, Int>()
        
        events.sortedBy { it.timestampUtc }.forEach { event ->
            val packageName = event.packageName
            val hourStartUtc = roundToHourStart(event.timestampUtc)
            val key = packageName to hourStartUtc
            
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Track session start
                    activeSessionsMap[packageName] = event.timestampUtc
                    
                    // Increment launch count
                    launchCountMap[key] = (launchCountMap[key] ?: 0) + 1
                }
                
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    // Calculate session duration
                    val sessionStart = activeSessionsMap.remove(packageName)
                    if (sessionStart != null && event.timestampUtc > sessionStart) {
                        val duration = event.timestampUtc - sessionStart
                        
                        // Handle sessions that span multiple hours
                        distributeUsageAcrossHours(
                            packageName,
                            sessionStart,
                            event.timestampUtc,
                            hourlyUsageMap
                        )
                    }
                }
            }
        }
        
        // Handle any active sessions that didn't close
        val currentTimeUtc = System.currentTimeMillis()
        activeSessionsMap.forEach { (packageName, sessionStart) ->
            if (currentTimeUtc > sessionStart) {
                distributeUsageAcrossHours(
                    packageName,
                    sessionStart,
                    currentTimeUtc,
                    hourlyUsageMap
                )
            }
        }
        
        // Merge launch counts with usage durations
        val result = mutableMapOf<Pair<String, Long>, Pair<Long, Int>>()
        hourlyUsageMap.forEach { (key, duration) ->
            val launchCount = launchCountMap[key] ?: 0
            result[key] = duration to launchCount
        }
        
        // Add entries that have launch counts but no duration
        launchCountMap.forEach { (key, count) ->
            if (!result.containsKey(key)) {
                result[key] = 0L to count
            }
        }
        
        return result
    }
    
    /**
     * Distributes usage duration across multiple hours if a session spans hour boundaries.
     */
    private fun distributeUsageAcrossHours(
        packageName: String,
        startUtc: Long,
        endUtc: Long,
        hourlyUsageMap: MutableMap<Pair<String, Long>, Long>
    ) {
    var currentHourStart = roundToHourStart(startUtc)
    val endHourStart = roundToHourStart(endUtc)
        
        while (currentHourStart <= endHourStart) {
            val hourEnd = currentHourStart + TimeUnit.HOURS.toMillis(1)
            val sessionStart = maxOf(startUtc, currentHourStart)
            val sessionEnd = minOf(endUtc, hourEnd)
            
            if (sessionEnd > sessionStart) {
                val duration = sessionEnd - sessionStart
                val key = packageName to currentHourStart
                hourlyUsageMap[key] = (hourlyUsageMap[key] ?: 0L) + duration
            }
            
            currentHourStart = hourEnd
        }
    }

    /**
     * Aggregates KEYGUARD unlock events into hourly buckets per day.
     */
    private fun aggregateDailyUnlocks(
        events: List<com.librefocus.models.UsageEventData>
    ): Map<Long, Map<Int, Int>> {
        if (events.isEmpty()) return emptyMap()

        val dailyUnlocks = mutableMapOf<Long, MutableMap<Int, Int>>()
        events.forEach { event ->
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                val dayStart = roundToDayStart(event.timestampUtc)
                val hour = extractUtcHourOfDay(event.timestampUtc).coerceIn(0, 23)
                val hourMap = dailyUnlocks.getOrPut(dayStart) { mutableMapOf() }
                hourMap[hour] = (hourMap[hour] ?: 0) + 1
            }
        }

        return dailyUnlocks.mapValues { entry -> entry.value.toSortedMap() }
    }
    
    /**
     * Saves hourly aggregated usage data to the database.
     */
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
                val appName = usageStatsDataSource.getAppName(packageName)
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
                // Check if usage record already exists
                val existingUsage = hourlyAppUsageDao.getUsageForAppAtHour(app.id, hourStartUtc)
                
                if (existingUsage != null) {
                    // Update existing record by adding to duration and launch count
                    hourlyAppUsageDao.updateUsage(
                        existingUsage.copy(
                            usageDurationMillis = existingUsage.usageDurationMillis + usageDuration,
                            launchCount = existingUsage.launchCount + launchCount
                        )
                    )
                } else {
                    // Insert new record
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
    }

    /**
     * Persists aggregated daily unlock information while avoiding unnecessary rewrites.
     */
    private suspend fun saveDailyUnlockData(
        dailyUnlockMap: Map<Long, Map<Int, Int>>,
        syncCompletionUtc: Long
    ) {
        if (dailyUnlockMap.isEmpty()) return

        val currentDayStart = roundToDayStart(syncCompletionUtc)
        val now = System.currentTimeMillis()

        dailyUnlockMap.forEach { (dayStartUtc, hourlyUnlocks) ->
            if (hourlyUnlocks.isEmpty()) return@forEach

            val existing = dailyDeviceUsageDao.getUsageForDate(dayStartUtc)
            if (existing != null && dayStartUtc < currentDayStart) {
                // Historical day already stored; skip to avoid double counting.
                return@forEach
            }

            val sortedUnlocks = hourlyUnlocks.toSortedMap()
            val entity = DailyDeviceUsageEntity(
                id = existing?.id ?: 0,
                dateUtc = dayStartUtc,
                hourlyUnlocks = sortedUnlocks,
                totalUnlocks = sortedUnlocks.values.sum(),
                lastUpdatedUtc = now
            )
            dailyDeviceUsageDao.insertOrUpdateDailyUsage(entity)
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
     * Returns a map of hour (0-23) to unlock count for the provided day (UTC midnight).
     */
    suspend fun getHourlyUnlocksForDay(dateUtc: Long): Map<Int, Int> {
        val dayStart = roundToDayStart(dateUtc)
        return dailyDeviceUsageDao.getUsageForDate(dayStart)?.hourlyUnlocks ?: emptyMap()
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
     * Retrieves total usage for all apps in a time range.
     */
    suspend fun getTotalUsageInTimeRange(startUtc: Long, endUtc: Long): Long {
        return hourlyAppUsageDao.getTotalUsageInTimeRange(startUtc, endUtc) ?: 0L
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
     * Calculates average app usage (duration and launch count) for the given month.
     * Month is 1-based (January = 1).
     */
    suspend fun getAverageAppUsageForMonth(year: Int, month: Int): AppUsageAverages = withContext(Dispatchers.IO) {
        val yearMonth = YearMonth.of(year, month)
        val startUtc = yearMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val endUtc = yearMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

        val totalUsage = hourlyAppUsageDao.getUsageInTimeRangeOnce(startUtc, endUtc)
        if (totalUsage.isEmpty()) {
            return@withContext AppUsageAverages(averageUsageMillis = 0L, averageLaunchCount = 0)
        }

        val totalDuration = totalUsage.sumOf { it.usageDurationMillis }
        val totalLaunches = totalUsage.sumOf { it.launchCount }

        // Calculate averages per day of data available in the month
        val daysInMonth = yearMonth.lengthOfMonth()
        AppUsageAverages(
            averageUsageMillis = totalDuration / daysInMonth,
            averageLaunchCount = (totalLaunches.toDouble() / daysInMonth).roundToInt()
        )
    }

    /**
     * Calculates average unlock count for the given month based on stored daily usage entities.
     */
    suspend fun getAverageUnlocksForMonth(year: Int, month: Int): Int = withContext(Dispatchers.IO) {
        val yearMonth = YearMonth.of(year, month)
        val startUtc = yearMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val endUtc = yearMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

        val dailyUsage = dailyDeviceUsageDao.getUsageForDayRange(startUtc, endUtc)
        if (dailyUsage.isEmpty()) return@withContext 0

        val totalUnlocks = dailyUsage.sumOf { it.totalUnlocks }
        val daysInMonth = yearMonth.lengthOfMonth()
        (totalUnlocks.toDouble() / daysInMonth).roundToInt()
    }
}
