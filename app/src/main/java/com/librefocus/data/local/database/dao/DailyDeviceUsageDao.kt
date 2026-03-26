package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.librefocus.data.local.database.entity.DailyDeviceUsageEntity

/**
 * DAO for accessing daily device unlock information.
 */
@Dao
interface DailyDeviceUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyUsage(entity: DailyDeviceUsageEntity)

    @Query(
        """
        SELECT * FROM daily_device_usage
        WHERE dateUtc >= :startUtc AND dateUtc < :endUtc
        ORDER BY dateUtc ASC
        """
    )
    suspend fun getUsageForDayRange(startUtc: Long, endUtc: Long): List<DailyDeviceUsageEntity>

    @Query(
        "SELECT * FROM daily_device_usage WHERE dateUtc = :dateUtc LIMIT 1"
    )
    suspend fun getUsageForDate(dateUtc: Long): DailyDeviceUsageEntity?
    
    // Synchronous methods for backup/restore operations
    @Query("SELECT * FROM daily_device_usage ORDER BY dateUtc ASC")
    fun getAllDailyUsage(): kotlinx.coroutines.flow.Flow<List<DailyDeviceUsageEntity>>
    
    @Query("SELECT * FROM daily_device_usage ORDER BY dateUtc ASC")
    fun getAllDailyUsageSync(): List<DailyDeviceUsageEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDailyUsageSync(entities: List<DailyDeviceUsageEntity>)
    
    @Query("DELETE FROM daily_device_usage WHERE id = :id")
    fun deleteDailyUsageSync(id: Int)
    
    @Query("DELETE FROM daily_device_usage")
    fun deleteAllDailyUsage()
}
