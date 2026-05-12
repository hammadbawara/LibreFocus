package com.librefocus.data.repository

import android.content.Context
import com.librefocus.data.local.database.UsageDatabase
import com.librefocus.data.local.database.dao.AchievementDao
import com.librefocus.data.local.database.dao.AppCategoryDao
import com.librefocus.data.local.database.dao.AppDao
import com.librefocus.data.local.database.dao.DailyDeviceUsageDao
import com.librefocus.data.local.database.dao.GoalHistoryDao
import com.librefocus.data.local.database.dao.HourlyAppUsageDao
import com.librefocus.data.local.database.dao.LimitDao
import com.librefocus.data.local.database.dao.PerfectDayDao
import com.librefocus.data.local.database.dao.SyncMetadataDao
import com.librefocus.models.BackupData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRestoreRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: UsageDatabase
    private lateinit var appCategoryDao: AppCategoryDao
    private lateinit var appDao: AppDao
    private lateinit var hourlyAppUsageDao: HourlyAppUsageDao
    private lateinit var dailyDeviceUsageDao: DailyDeviceUsageDao
    private lateinit var limitDao: LimitDao
    private lateinit var syncMetadataDao: SyncMetadataDao
    private lateinit var perfectDayDao: PerfectDayDao
    private lateinit var achievementDao: AchievementDao
    private lateinit var goalHistoryDao: GoalHistoryDao

    private lateinit var repository: BackupRestoreRepository

    @Before
    fun setup() {
        context = mockk()
        database = mockk()
        appCategoryDao = mockk()
        appDao = mockk()
        hourlyAppUsageDao = mockk()
        dailyDeviceUsageDao = mockk()
        limitDao = mockk()
        syncMetadataDao = mockk()
        perfectDayDao = mockk()
        achievementDao = mockk()
        goalHistoryDao = mockk()

        repository = BackupRestoreRepository(
            context = context,
            database = database,
            appCategoryDao = appCategoryDao,
            appDao = appDao,
            hourlyAppUsageDao = hourlyAppUsageDao,
            dailyDeviceUsageDao = dailyDeviceUsageDao,
            limitDao = limitDao,
            syncMetadataDao = syncMetadataDao,
            perfectDayDao = perfectDayDao,
            achievementDao = achievementDao,
            goalHistoryDao = goalHistoryDao
        )
    }

    @Test
    fun `createBackup successfully pulls data from all DAOs`() = runTest {
        coEvery { appCategoryDao.getAllCategories() } returns flowOf(emptyList())
        coEvery { appDao.getAllApps() } returns flowOf(emptyList())
        coEvery { hourlyAppUsageDao.getAllUsage() } returns flowOf(emptyList())
        coEvery { dailyDeviceUsageDao.getAllDailyUsage() } returns flowOf(emptyList())
        coEvery { limitDao.getAllLimits() } returns flowOf(emptyList())
        coEvery { limitDao.getAllScheduleLimits() } returns flowOf(emptyList())
        coEvery { limitDao.getAllUsageLimits() } returns flowOf(emptyList())
        coEvery { limitDao.getAllLaunchCountLimits() } returns flowOf(emptyList())
        coEvery { syncMetadataDao.getAllMetadata() } returns flowOf(emptyList())
        coEvery { perfectDayDao.getAllPerfectDays() } returns flowOf(emptyList())
        coEvery { achievementDao.getAllAchievements() } returns flowOf(emptyList())
        coEvery { goalHistoryDao.getAllGoals() } returns flowOf(emptyList())

        val result = repository.createBackup()

        assertTrue(result.isSuccess)
        val backup = result.getOrNull()!!
        assertEquals(2, backup.version)
        // Data is empty but not null
        assertTrue(backup.database.appCategories.isEmpty())
        assertTrue(backup.database.apps.isEmpty())
    }

    @Test
    fun `resetAllData clears all DAOs via transaction`() = runTest {
        every { database.runInTransaction(any<Runnable>()) } answers {
            (it.invocation.args[0] as Runnable).run()
        }

        coEvery { hourlyAppUsageDao.deleteAllUsage() } returns Unit
        coEvery { appDao.deleteAllApps() } returns Unit
        coEvery { appCategoryDao.deleteAllCategories() } returns Unit
        coEvery { dailyDeviceUsageDao.deleteAllDailyUsage() } returns Unit
        coEvery { limitDao.deleteAllScheduleLimits() } returns Unit
        coEvery { limitDao.deleteAllUsageLimits() } returns Unit
        coEvery { limitDao.deleteAllLaunchCountLimits() } returns Unit
        coEvery { limitDao.deleteAllLimits() } returns Unit
        coEvery { syncMetadataDao.deleteAllMetadata() } returns Unit
        coEvery { perfectDayDao.deleteAllPerfectDays() } returns Unit
        coEvery { achievementDao.deleteAllAchievements() } returns Unit
        coEvery { goalHistoryDao.deleteAllGoals() } returns Unit

        val result = repository.resetAllData()

        assertTrue(result.isSuccess)

        coVerify { hourlyAppUsageDao.deleteAllUsage() }
        coVerify { appDao.deleteAllApps() }
        coVerify { limitDao.deleteAllLimits() }
        coVerify { perfectDayDao.deleteAllPerfectDays() }
    }

    @Test
    fun `restoreBackup clears and restores data from backup`() = runTest {
        every { database.runInTransaction(any<Runnable>()) } answers {
            (it.invocation.args[0] as Runnable).run()
        }

        // Mocks for delete
        coEvery { hourlyAppUsageDao.deleteAllUsage() } returns Unit
        coEvery { appDao.deleteAllApps() } returns Unit
        coEvery { appCategoryDao.deleteAllCategories() } returns Unit
        coEvery { dailyDeviceUsageDao.deleteAllDailyUsage() } returns Unit
        coEvery { limitDao.deleteAllScheduleLimits() } returns Unit
        coEvery { limitDao.deleteAllUsageLimits() } returns Unit
        coEvery { limitDao.deleteAllLaunchCountLimits() } returns Unit
        coEvery { limitDao.deleteAllLimits() } returns Unit
        coEvery { syncMetadataDao.deleteAllMetadata() } returns Unit
        coEvery { perfectDayDao.deleteAllPerfectDays() } returns Unit
        coEvery { achievementDao.deleteAllAchievements() } returns Unit
        coEvery { goalHistoryDao.deleteAllGoals() } returns Unit

        // Mocks for insert
        coEvery { appCategoryDao.insertCategoriesSync(any()) } returns emptyList()
        coEvery { appDao.insertAppsSync(any()) } returns emptyList()
        coEvery { hourlyAppUsageDao.insertUsageSync(any()) } returns emptyList()
        coEvery { dailyDeviceUsageDao.insertDailyUsageSync(any()) } returns Unit
        coEvery { limitDao.insertLimitsSync(any()) } returns Unit
        coEvery { limitDao.insertScheduleLimitsSync(any()) } returns Unit
        coEvery { limitDao.insertUsageLimitsSync(any()) } returns Unit
        coEvery { limitDao.insertLaunchCountLimitsSync(any()) } returns Unit
        coEvery { syncMetadataDao.insertMetadataSync(any()) } returns Unit
        coEvery { perfectDayDao.upsertPerfectDaysSync(any()) } returns emptyList()
        coEvery { achievementDao.insertAchievementsSync(any()) } returns emptyList()
        coEvery { goalHistoryDao.insertGoalsSync(any()) } returns emptyList()

        val mockBackupData = mockk<BackupData>(relaxed = true)

        val result = repository.restoreBackup(mockBackupData)

        assertTrue(result.isSuccess)

        coVerify(exactly = 1) { appDao.deleteAllApps() }
        coVerify(exactly = 1) { appDao.insertAppsSync(any()) }
        coVerify(exactly = 1) { perfectDayDao.upsertPerfectDaysSync(any()) }
    }
}
