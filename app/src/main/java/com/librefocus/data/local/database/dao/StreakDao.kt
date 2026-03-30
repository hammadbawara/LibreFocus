package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.librefocus.data.local.database.entity.StreakEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for streak tracking.
 */
@Dao
interface StreakDao {
    
    @Query("SELECT * FROM streaks ORDER BY date DESC")
    fun getAllStreaks(): Flow<List<StreakEntity>>
    
    @Query("SELECT * FROM streaks WHERE date = :date LIMIT 1")
    suspend fun getStreakForDate(date: Long): StreakEntity?
    
    @Query("SELECT * FROM streaks WHERE goalMet = 1 ORDER BY date DESC")
    suspend fun getSuccessfulStreaks(): List<StreakEntity>
    
    @Query("""
        SELECT * FROM streaks 
        WHERE goalMet = 1 
        ORDER BY date DESC 
        LIMIT :days
    """)
    suspend fun getRecentSuccessfulStreaks(days: Int = 30): List<StreakEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: StreakEntity)
    
    @Update
    suspend fun updateStreak(streak: StreakEntity)
    
    @Delete
    suspend fun deleteStreak(streak: StreakEntity)
    
    @Query("DELETE FROM streaks")
    suspend fun clear()
}

