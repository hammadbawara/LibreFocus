package com.librefocus.data.repository

import android.content.Context
import android.net.Uri
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
import com.librefocus.models.DatabaseBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.InputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRestoreRepository(
    private val context: Context,
    private val database: UsageDatabase,
    private val appCategoryDao: AppCategoryDao,
    private val appDao: AppDao,
    private val hourlyAppUsageDao: HourlyAppUsageDao,
    private val dailyDeviceUsageDao: DailyDeviceUsageDao,
    private val limitDao: LimitDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val perfectDayDao: PerfectDayDao,
    private val achievementDao: AchievementDao,
    private val goalHistoryDao: GoalHistoryDao
) {
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    /**
     * Creates a backup of all database tables.
     */
    suspend fun createBackup(): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val databaseBackup = DatabaseBackup(
                appCategories = appCategoryDao.getAllCategories().first(),
                apps = appDao.getAllApps().first(),
                hourlyAppUsage = hourlyAppUsageDao.getAllUsage().first(),
                dailyDeviceUsage = dailyDeviceUsageDao.getAllDailyUsage().first(),
                limits = limitDao.getAllLimits().first(),
                scheduleLimits = limitDao.getAllScheduleLimits().first(),
                usageLimits = limitDao.getAllUsageLimits().first(),
                launchCountLimits = limitDao.getAllLaunchCountLimits().first(),
                syncMetadata = syncMetadataDao.getAllMetadata().first(),
                perfectDays = perfectDayDao.getAllPerfectDays().first(),
                achievements = achievementDao.getAllAchievements().first(),
                goalHistory = goalHistoryDao.getAllGoals().first()
            )

            val backupData = BackupData(
                version = 2,
                timestamp = System.currentTimeMillis(),
                database = databaseBackup
            )

            Result.success(backupData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports backup data to a ZIP file at the specified URI.
     */
    suspend fun exportBackup(uri: Uri, data: BackupData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                    zipOut.setLevel(Deflater.BEST_COMPRESSION)
                    
                    val jsonString = json.encodeToString(data)
                    val entry = ZipEntry("backup.json")
                    zipOut.putNextEntry(entry)
                    zipOut.write(jsonString.toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports backup data from a ZIP file at the specified URI.
     */
    suspend fun importBackup(uri: Uri): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = extractJsonFromZip(inputStream)
                val backupData = json.decodeFromString<BackupData>(jsonString)
                
                // Validate backup version
                if (backupData.version !in setOf(1, 2)) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Unsupported backup version: ${backupData.version}")
                    )
                }
                
                Result.success(backupData)
            } ?: Result.failure(IllegalStateException("Cannot open backup file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores backup data by deleting all existing data and importing new data.
     * This is a complete replacement operation.
     */
    suspend fun restoreBackup(data: BackupData): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                database.runInTransaction {
                    // Delete all existing data (order matters due to foreign keys)
                    hourlyAppUsageDao.deleteAllUsage()
                    appDao.deleteAllApps()
                    appCategoryDao.deleteAllCategories()
                    dailyDeviceUsageDao.deleteAllDailyUsage()
                    limitDao.deleteAllScheduleLimits()
                    limitDao.deleteAllUsageLimits()
                    limitDao.deleteAllLaunchCountLimits()
                    limitDao.deleteAllLimits()
                    syncMetadataDao.deleteAllMetadata()
                    perfectDayDao.deleteAllPerfectDays()
                    achievementDao.deleteAllAchievements()
                    goalHistoryDao.deleteAllGoals()
                    
                    // Import all data from backup
                    appCategoryDao.insertCategoriesSync(data.database.appCategories)
                    appDao.insertAppsSync(data.database.apps)
                    hourlyAppUsageDao.insertUsageSync(data.database.hourlyAppUsage)
                    dailyDeviceUsageDao.insertDailyUsageSync(data.database.dailyDeviceUsage)
                    limitDao.insertLimitsSync(data.database.limits)
                    limitDao.insertScheduleLimitsSync(data.database.scheduleLimits)
                    limitDao.insertUsageLimitsSync(data.database.usageLimits)
                    limitDao.insertLaunchCountLimitsSync(data.database.launchCountLimits)
                    syncMetadataDao.insertMetadataSync(data.database.syncMetadata)
                    perfectDayDao.upsertPerfectDaysSync(data.database.perfectDays)
                    achievementDao.insertAchievementsSync(data.database.achievements)
                    goalHistoryDao.insertGoalsSync(data.database.goalHistory)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Resets all database data.
     */
    suspend fun resetAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.runInTransaction {
                // Clear all tables (order matters due to foreign keys)
                hourlyAppUsageDao.deleteAllUsage()
                appDao.deleteAllApps()
                appCategoryDao.deleteAllCategories()
                dailyDeviceUsageDao.deleteAllDailyUsage()
                limitDao.deleteAllScheduleLimits()
                limitDao.deleteAllUsageLimits()
                limitDao.deleteAllLaunchCountLimits()
                limitDao.deleteAllLimits()
                syncMetadataDao.deleteAllMetadata()
                perfectDayDao.deleteAllPerfectDays()
                achievementDao.deleteAllAchievements()
                goalHistoryDao.deleteAllGoals()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJsonFromZip(inputStream: InputStream): String {
        ZipInputStream(inputStream).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (entry.name == "backup.json") {
                    return zipIn.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
                entry = zipIn.nextEntry
            }
        }
        throw IllegalArgumentException("backup.json not found in ZIP file")
    }
}
