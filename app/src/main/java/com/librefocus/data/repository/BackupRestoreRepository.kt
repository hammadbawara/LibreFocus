package com.librefocus.data.repository

import android.content.Context
import android.net.Uri
import com.librefocus.data.local.database.UsageDatabase
import com.librefocus.data.local.database.dao.AppCategoryDao
import com.librefocus.data.local.database.dao.AppDao
import com.librefocus.data.local.database.dao.HourlyAppUsageDao
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
    private val syncMetadataDao: SyncMetadataDao
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Creates a backup of essential usage data (categories, apps, hourly usage, sync metadata).
     */
    suspend fun createBackup(): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val databaseBackup = DatabaseBackup(
                appCategories = appCategoryDao.getAllCategories().first(),
                apps = appDao.getAllApps().first(),
                hourlyAppUsage = hourlyAppUsageDao.getAllUsage().first(),
                syncMetadata = syncMetadataDao.getAllMetadata().first()
            )

            val backupData = BackupData(
                version = 1,
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
                if (backupData.version != 1) {
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
     * Restores backup data with conflict resolution strategy.
     * @param data Backup data to restore
     * @param overrideConflicts If true, imported data overrides existing; if false, existing data is kept
     */
    suspend fun restoreBackup(data: BackupData, overrideConflicts: Boolean): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                database.runInTransaction {
                    // Categories: detect conflicts by categoryName
                    val existingCategories = appCategoryDao.getAllCategoriesSync()
                    val existingCategoryNames = existingCategories.map { it.categoryName }.toSet()
                    
                    val (conflictingCategories, newCategories) = data.database.appCategories
                        .partition { it.categoryName in existingCategoryNames }
                    
                    if (overrideConflicts) {
                        // Delete existing conflicting categories (cascade deletes related data)
                        conflictingCategories.forEach { imported ->
                            existingCategories.find { it.categoryName == imported.categoryName }?.let {
                                appCategoryDao.deleteCategorySync(it.id)
                            }
                        }
                        // Insert all imported categories (including previously conflicting ones)
                        appCategoryDao.insertCategoriesSync(data.database.appCategories)
                    } else {
                        // Only insert non-conflicting categories
                        appCategoryDao.insertCategoriesSync(newCategories)
                    }

                    // Apps: detect conflicts by packageName
                    val existingApps = appDao.getAllAppsSync()
                    val existingPackageNames = existingApps.map { it.packageName }.toSet()
                    
                    val (conflictingApps, newApps) = data.database.apps
                        .partition { it.packageName in existingPackageNames }
                    
                    if (overrideConflicts) {
                        conflictingApps.forEach { imported ->
                            existingApps.find { it.packageName == imported.packageName }?.let {
                                appDao.deleteAppSync(it.id)
                            }
                        }
                        appDao.insertAppsSync(data.database.apps)
                    } else {
                        appDao.insertAppsSync(newApps)
                    }

                    // Hourly Usage: detect conflicts by (appId, hourStartUtc)
                    val existingUsage = hourlyAppUsageDao.getAllUsageSync()
                    val existingUsageKeys = existingUsage.map { "${it.appId}_${it.hourStartUtc}" }.toSet()
                    
                    val (conflictingUsage, newUsage) = data.database.hourlyAppUsage
                        .partition { "${it.appId}_${it.hourStartUtc}" in existingUsageKeys }
                    
                    if (overrideConflicts) {
                        conflictingUsage.forEach { imported ->
                            existingUsage.find { 
                                it.appId == imported.appId && it.hourStartUtc == imported.hourStartUtc 
                            }?.let {
                                hourlyAppUsageDao.deleteUsageSync(it.id)
                            }
                        }
                        hourlyAppUsageDao.insertUsageSync(data.database.hourlyAppUsage)
                    } else {
                        hourlyAppUsageDao.insertUsageSync(newUsage)
                    }

                    // Sync Metadata: detect conflicts by key
                    val existingMetadata = syncMetadataDao.getAllMetadataSync()
                    val existingKeys = existingMetadata.map { it.key }.toSet()
                    
                    val (conflictingMetadata, newMetadata) = data.database.syncMetadata
                        .partition { it.key in existingKeys }
                    
                    if (overrideConflicts) {
                        conflictingMetadata.forEach { imported ->
                            syncMetadataDao.upsertSync(imported)
                        }
                        syncMetadataDao.insertMetadataSync(newMetadata)
                    } else {
                        syncMetadataDao.insertMetadataSync(newMetadata)
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Resets essential usage data (clears categories, apps, hourly usage, and sync metadata).
     */
    suspend fun resetAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.runInTransaction {
                // Clear essential tables (order matters due to foreign keys)
                hourlyAppUsageDao.deleteAllUsage()
                appDao.deleteAllApps()
                appCategoryDao.deleteAllCategories()
                syncMetadataDao.deleteAllMetadata()
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
