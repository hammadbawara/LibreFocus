package com.librefocus.data.repository

import android.util.Log
import com.librefocus.data.local.AppInfoProvider
import com.librefocus.data.local.UsageStatsProvider
import com.librefocus.data.local.database.dao.AppCategoryDao
import com.librefocus.data.local.database.dao.AppDao
import com.librefocus.data.local.database.dao.DailyDeviceUsageDao
import com.librefocus.data.local.database.dao.HourlyAppUsageDao
import com.librefocus.data.local.database.dao.SyncMetadataDao
import com.librefocus.data.local.database.entity.AppCategoryEntity
import com.librefocus.data.local.database.entity.AppEntity
import com.librefocus.data.local.database.entity.HourlyAppUsageEntity
import com.librefocus.data.local.database.entity.SyncMetadataEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UsageTrackingRepositoryTest {

    private lateinit var usageStatsProvider: UsageStatsProvider
    private lateinit var appInfoProvider: AppInfoProvider
    private lateinit var appCategoryDao: AppCategoryDao
    private lateinit var appDao: AppDao
    private lateinit var hourlyAppUsageDao: HourlyAppUsageDao
    private lateinit var dailyDeviceUsageDao: DailyDeviceUsageDao
    private lateinit var syncMetadataDao: SyncMetadataDao

    private lateinit var repository: UsageTrackingRepository

    @Before
    fun setup() {
        usageStatsProvider = mockk()
        appInfoProvider = mockk()
        appCategoryDao = mockk()
        appDao = mockk()
        hourlyAppUsageDao = mockk()
        dailyDeviceUsageDao = mockk()
        syncMetadataDao = mockk()

        repository = UsageTrackingRepository(
            usageStatsProvider = usageStatsProvider,
            appInfoProvider = appInfoProvider,
            appCategoryDao = appCategoryDao,
            appDao = appDao,
            hourlyAppUsageDao = hourlyAppUsageDao,
            dailyDeviceUsageDao = dailyDeviceUsageDao,
            syncMetadataDao = syncMetadataDao
        )

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `syncUsageStats successfully fetches stats and saves them`() = runTest {
        val lastSyncTime = 1000L
        coEvery { syncMetadataDao.getMetadata("last_sync_timestamp_utc") } returns SyncMetadataEntity("last_sync_timestamp_utc", lastSyncTime)
        
        // Mock usageStatsProvider
        val hourlyUsageMap = mapOf<Pair<String, Long>, Pair<Long, Int>>(
            Pair("com.example.app", 2000L) to Pair(5000L, 2)
        )
        every { usageStatsProvider.getHourlyUsageStatistics(any(), any()) } returns hourlyUsageMap
        
        // Mock App Dao
        val appEntity = AppEntity(id = 1, packageName = "com.example.app", appName = "Example App", categoryId = 1)
        coEvery { appDao.getAppByPackageName("com.example.app") } returns appEntity
        
        // Mock Hourly App Usage Dao
        coEvery { hourlyAppUsageDao.insertUsage(any()) } returns 1L
        
        // Mock Sync Metadata Update
        coEvery { syncMetadataDao.insertMetadata(any()) } returns Unit

        repository.syncUsageStats()

        coVerify { appDao.getAppByPackageName("com.example.app") }
        coVerify { hourlyAppUsageDao.insertUsage(match {
            it.appId == 1 && it.hourStartUtc == 2000L && it.usageDurationMillis == 5000L && it.launchCount == 2
        }) }
        coVerify { syncMetadataDao.insertMetadata(match {
            it.key == "last_sync_timestamp_utc"
        }) }
    }

    @Test
    fun `syncUsageStats saves new app and category if not exist`() = runTest {
        coEvery { syncMetadataDao.getMetadata(any()) } returns null
        
        val hourlyUsageMap = mapOf(
            Pair("com.new.app", 3000L) to Pair(1000L, 1)
        )
        every { usageStatsProvider.getHourlyUsageStatistics(any(), any()) } returns hourlyUsageMap

        // Mock App Dao missing app
        var getAppCallCount = 0
        coEvery { appDao.getAppByPackageName("com.new.app") } answers { null }
        
        // Mock providers
        coEvery { appInfoProvider.getAppName("com.new.app") } returns "New App"
        coEvery { appInfoProvider.getAppCategory("com.new.app") } returns "Productivity"
        
        // Mock Category lookup and creation
        coEvery { appCategoryDao.getCategoryByName("Productivity") } returns null
        coEvery { appCategoryDao.insertCategory(any()) } returns 2L
        coEvery { appCategoryDao.getCategoryById(2) } returns AppCategoryEntity(id = 2, categoryName = "Productivity", isCustom = false, addedAtUtc = 0L)
        
        // Mock App creation
        coEvery { appDao.insertApp(any()) } returns 3L
        // Return null first time, let it be created, then return new app when queried by ID
        coEvery { appDao.getAppById(3) } returns AppEntity(id = 3, packageName = "com.new.app", appName = "New App", categoryId = 2)
        
        coEvery { hourlyAppUsageDao.insertUsage(any()) } returns 1L
        coEvery { syncMetadataDao.insertMetadata(any()) } returns Unit

        repository.syncUsageStats()

        coVerify { appCategoryDao.insertCategory(match { it.categoryName == "Productivity" }) }
        coVerify { appDao.insertApp(match { it.packageName == "com.new.app" && it.categoryId == 2 }) }
        coVerify { hourlyAppUsageDao.insertUsage(match { it.appId == 3 && it.usageDurationMillis == 1000L }) }
    }

    @Test
    fun `getAppUsageSummaryInTimeRange returns aggregated and sorted list`() = runTest {
        val startUtc = 0L
        val endUtc = 10000L
        
        val app1 = AppEntity(id = 1, packageName = "pkg1", appName = "App 1", categoryId = 1)
        val app2 = AppEntity(id = 2, packageName = "pkg2", appName = "App 2", categoryId = 1)
        
        val usages = listOf(
            HourlyAppUsageEntity(id = 1, appId = 1, hourStartUtc = 1000, usageDurationMillis = 500, launchCount = 1),
            HourlyAppUsageEntity(id = 2, appId = 1, hourStartUtc = 2000, usageDurationMillis = 1000, launchCount = 1),
            HourlyAppUsageEntity(id = 3, appId = 2, hourStartUtc = 1000, usageDurationMillis = 800, launchCount = 3)
        )
        
        coEvery { hourlyAppUsageDao.getUsageInTimeRangeOnce(startUtc, endUtc) } returns usages
        coEvery { appDao.getAppById(1) } returns app1
        coEvery { appDao.getAppById(2) } returns app2
        
        val result = repository.getAppUsageSummaryInTimeRange(startUtc, endUtc)
        
        assertEquals(2, result.size)
        // Expected sort descending by total duration
        assertEquals("pkg1", result[0].packageName)
        assertEquals(1500L, result[0].usageDurationMillis)
        assertEquals(2, result[0].launchCount)
        
        assertEquals("pkg2", result[1].packageName)
        assertEquals(800L, result[1].usageDurationMillis)
        assertEquals(3, result[1].launchCount)
    }

    @Test
    fun `getAppUsageTotalsGroupedByHour aggregates data by hour correctly`() = runTest {
        val packageName = "com.test.app"
        val startUtc = 0L
        val endUtc = 20000L
        
        val app = AppEntity(id = 1, packageName = packageName, appName = "App", categoryId = 1)
        coEvery { appDao.getAppByPackageName(packageName) } returns app
        
        val usages = listOf(
            HourlyAppUsageEntity(id = 1, appId = 1, hourStartUtc = 0L, usageDurationMillis = 100L, launchCount = 1),
            HourlyAppUsageEntity(id = 2, appId = 1, hourStartUtc = 0L, usageDurationMillis = 150L, launchCount = 2),
            HourlyAppUsageEntity(id = 3, appId = 1, hourStartUtc = 3600000L, usageDurationMillis = 200L, launchCount = 1)
        )
        coEvery { hourlyAppUsageDao.getAppUsageInTimeRangeOnce(1, startUtc, endUtc) } returns usages
        
        val result = repository.getAppUsageTotalsGroupedByHour(packageName, startUtc, endUtc)
        
        assertEquals(2, result.size)
        assertEquals(0L, result[0].bucketStartUtc)
        assertEquals(250L, result[0].totalUsageMillis)
        assertEquals(3, result[0].totalLaunchCount)
        
        assertEquals(3600000L, result[1].bucketStartUtc)
        assertEquals(200L, result[1].totalUsageMillis)
        assertEquals(1, result[1].totalLaunchCount)
    }
}
