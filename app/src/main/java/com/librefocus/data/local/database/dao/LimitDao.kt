package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.librefocus.data.local.database.entity.LaunchCountEntity
import com.librefocus.data.local.database.entity.LimitEntity
import com.librefocus.data.local.database.entity.ScheduleLimitEntity
import com.librefocus.data.local.database.entity.UsageLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LimitDao {

    // ========== Insert Operations ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLimit(limit: LimitEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleLimit(scheduleLimit: ScheduleLimitEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageLimit(usageLimit: UsageLimitEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaunchCountLimit(launchCountLimit: LaunchCountEntity)
    
    /**
     * Inserts a complete limit with its type-specific data in a single transaction.
     */
    @Transaction
    suspend fun insertCompleteLimit(
        limit: LimitEntity,
        scheduleLimit: ScheduleLimitEntity? = null,
        usageLimit: UsageLimitEntity? = null,
        launchCountLimit: LaunchCountEntity? = null
    ) {
        insertLimit(limit)
        scheduleLimit?.let { insertScheduleLimit(it) }
        usageLimit?.let { insertUsageLimit(it) }
        launchCountLimit?.let { insertLaunchCountLimit(it) }
    }
    
    // ========== Update Operations ==========
    
    @Update
    suspend fun updateLimit(limit: LimitEntity)
    
    @Update
    suspend fun updateScheduleLimit(scheduleLimit: ScheduleLimitEntity)
    
    @Update
    suspend fun updateUsageLimit(usageLimit: UsageLimitEntity)
    
    @Update
    suspend fun updateLaunchCountLimit(launchCountLimit: LaunchCountEntity)
    
    /**
     * Updates a complete limit with its type-specific data in a single transaction.
     * Old type-specific data is deleted and new data is inserted.
     */
    @Transaction
    suspend fun updateCompleteLimit(
        limit: LimitEntity,
        scheduleLimit: ScheduleLimitEntity? = null,
        usageLimit: UsageLimitEntity? = null,
        launchCountLimit: LaunchCountEntity? = null
    ) {
        updateLimit(limit)
        
        // Delete old type-specific data
        deleteScheduleLimitByLimitId(limit.id)
        deleteUsageLimitByLimitId(limit.id)
        deleteLaunchCountLimitByLimitId(limit.id)
        
        // Insert new type-specific data
        scheduleLimit?.let { insertScheduleLimit(it) }
        usageLimit?.let { insertUsageLimit(it) }
        launchCountLimit?.let { insertLaunchCountLimit(it) }
    }
    
    @Query("UPDATE limits SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateLimitEnabledStatus(id: String, isEnabled: Boolean)
    
    // ========== Delete Operations ==========
    
    @Query("DELETE FROM limits WHERE id = :id")
    suspend fun deleteLimit(id: String)
    
    @Query("DELETE FROM schedule_limits WHERE limitId = :limitId")
    suspend fun deleteScheduleLimitByLimitId(limitId: String)
    
    @Query("DELETE FROM usage_limits WHERE limitId = :limitId")
    suspend fun deleteUsageLimitByLimitId(limitId: String)
    
    @Query("DELETE FROM launch_count_limits WHERE limitId = :limitId")
    suspend fun deleteLaunchCountLimitByLimitId(limitId: String)
    
    /**
     * Deletes a limit and all its related type-specific data (handled by CASCADE).
     */
    @Transaction
    suspend fun deleteCompleteLimit(id: String) {
        deleteLimit(id)
    }
    
    // ========== Query Operations ==========
    
    @Query("SELECT * FROM limits WHERE id = :id")
    suspend fun getLimitById(id: String): LimitEntity?
    
    @Query("SELECT * FROM schedule_limits WHERE limitId = :limitId")
    suspend fun getScheduleLimitByLimitId(limitId: String): ScheduleLimitEntity?
    
    @Query("SELECT * FROM usage_limits WHERE limitId = :limitId")
    suspend fun getUsageLimitByLimitId(limitId: String): UsageLimitEntity?
    
    @Query("SELECT * FROM launch_count_limits WHERE limitId = :limitId")
    suspend fun getLaunchCountLimitByLimitId(limitId: String): LaunchCountEntity?
    
    /**
     * Gets a complete limit with its type-specific data.
     * Returns null if the limit doesn't exist.
     */
    @Transaction
    suspend fun getCompleteLimitById(id: String): CompleteLimitData? {
        val limit = getLimitById(id) ?: return null
        return CompleteLimitData(
            limit = limit,
            scheduleLimit = getScheduleLimitByLimitId(id),
            usageLimit = getUsageLimitByLimitId(id),
            launchCountLimit = getLaunchCountLimitByLimitId(id)
        )
    }
    
    @Query("SELECT * FROM limits ORDER BY createdAt DESC")
    fun getAllLimits(): Flow<List<LimitEntity>>

    @Transaction
    suspend fun getAllCompleteLimits(): List<CompleteLimitData> {
        val limits = getAllLimitsAsList()
        return limits.map { limit ->
            CompleteLimitData(
                limit = limit,
                scheduleLimit = getScheduleLimitByLimitId(limit.id),
                usageLimit = getUsageLimitByLimitId(limit.id),
                launchCountLimit = getLaunchCountLimitByLimitId(limit.id)
            )
        }
    }
    
    @Query("SELECT * FROM limits ORDER BY createdAt DESC")
    suspend fun getAllLimitsAsList(): List<LimitEntity>
    
    @Query("SELECT COUNT(*) FROM limits")
    fun getLimitCount(): Flow<Int>
    
    @Query("SELECT * FROM limits WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getEnabledLimits(): Flow<List<LimitEntity>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM limits WHERE id = :id)")
    suspend fun isLimitExists(id: String): Boolean
}

/**
 * Data class to hold a complete limit with all its type-specific data.
 * Used internally by the DAO to return joined data.
 */
data class CompleteLimitData(
    val limit: LimitEntity,
    val scheduleLimit: ScheduleLimitEntity?,
    val usageLimit: UsageLimitEntity?,
    val launchCountLimit: LaunchCountEntity?
)
