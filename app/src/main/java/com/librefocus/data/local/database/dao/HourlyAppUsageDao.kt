package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.librefocus.data.local.database.entity.HourlyAppUsageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for hourly app usage data.
 */
@Dao
interface HourlyAppUsageDao {
    
    @Query("SELECT * FROM hourly_app_usage WHERE appId = :appId ORDER BY hourStartUtc DESC")
    fun getUsageByApp(appId: Int): Flow<List<HourlyAppUsageEntity>>
    
    @Query("""
        SELECT * FROM hourly_app_usage 
        WHERE hourStartUtc >= :startUtc AND hourStartUtc < :endUtc
        ORDER BY hourStartUtc ASC
    """)
    fun getUsageInTimeRange(startUtc: Long, endUtc: Long): Flow<List<HourlyAppUsageEntity>>
    
    @Query("""
        SELECT * FROM hourly_app_usage 
        WHERE appId = :appId AND hourStartUtc >= :startUtc AND hourStartUtc < :endUtc
        ORDER BY hourStartUtc ASC
    """)
    fun getAppUsageInTimeRange(
        appId: Int,
        startUtc: Long,
        endUtc: Long
    ): Flow<List<HourlyAppUsageEntity>>

    @Query("""
        SELECT * FROM hourly_app_usage
        WHERE hourStartUtc >= :startUtc AND hourStartUtc < :endUtc
    """)
    suspend fun getUsageInTimeRangeOnce(startUtc: Long, endUtc: Long): List<HourlyAppUsageEntity>
    
    @Query("""
        SELECT * FROM hourly_app_usage 
        WHERE appId = :appId AND hourStartUtc >= :startUtc AND hourStartUtc < :endUtc
        ORDER BY hourStartUtc ASC
    """)
    suspend fun getAppUsageInTimeRangeOnce(
        appId: Int,
        startUtc: Long,
        endUtc: Long
    ): List<HourlyAppUsageEntity>
    
    @Query("""
        SELECT * FROM hourly_app_usage 
        WHERE appId = :appId AND hourStartUtc = :hourStartUtc
        LIMIT 1
    """)
    suspend fun getUsageForAppAtHour(appId: Int, hourStartUtc: Long): HourlyAppUsageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: HourlyAppUsageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsages(usages: List<HourlyAppUsageEntity>): List<Long>
    
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUsage(usage: HourlyAppUsageEntity)
    
    @Delete
    suspend fun deleteUsage(usage: HourlyAppUsageEntity)
    
    @Query("DELETE FROM hourly_app_usage WHERE hourStartUtc < :beforeUtc")
    suspend fun deleteUsagesBefore(beforeUtc: Long): Int
    
    @Query("""
        SELECT SUM(usageDurationMillis) FROM hourly_app_usage 
        WHERE hourStartUtc >= :startUtc AND hourStartUtc < :endUtc
    """)
    suspend fun getTotalUsageInTimeRange(startUtc: Long, endUtc: Long): Long?
    
    @Query("""
        SELECT SUM(usageDurationMillis) FROM hourly_app_usage 
        WHERE appId = :appId AND hourStartUtc >= :startUtc AND hourStartUtc < :endUtc
    """)
    suspend fun getAppTotalUsageInTimeRange(
        appId: Int,
        startUtc: Long,
        endUtc: Long
    ): Long?
}
