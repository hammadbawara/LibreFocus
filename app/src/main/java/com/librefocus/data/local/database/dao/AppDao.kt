package com.librefocus.data.local.database.dao

import androidx.room.*
import com.librefocus.data.local.database.entity.AppEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for apps.
 */
@Dao
interface AppDao {
    
    @Query("SELECT * FROM apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppEntity>>
    
    @Query("SELECT * FROM apps WHERE id = :appId")
    suspend fun getAppById(appId: Int): AppEntity?
    
    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackageName(packageName: String): AppEntity?
    
    @Query("SELECT * FROM apps WHERE categoryId = :categoryId")
    fun getAppsByCategory(categoryId: Int): Flow<List<AppEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>): List<Long>
    
    @Update
    suspend fun updateApp(app: AppEntity)
    
    @Delete
    suspend fun deleteApp(app: AppEntity)
    
    @Query("DELETE FROM apps WHERE id = :appId")
    suspend fun deleteAppById(appId: Int)
    
    @Query("SELECT EXISTS(SELECT 1 FROM apps WHERE packageName = :packageName)")
    suspend fun isAppExists(packageName: String): Boolean
}
