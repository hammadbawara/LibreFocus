package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.librefocus.data.local.database.entity.GoalHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalHistoryDao {

    @Query("SELECT * FROM goal_history ORDER BY startDateUtc ASC")
    fun getAllGoals(): Flow<List<GoalHistoryEntity>>

    @Query("SELECT * FROM goal_history ORDER BY startDateUtc ASC")
    fun getAllGoalsSync(): List<GoalHistoryEntity>

    @Query("SELECT * FROM goal_history WHERE startDateUtc <= :dateUtc AND (endDateUtc IS NULL OR endDateUtc > :dateUtc) ORDER BY startDateUtc DESC LIMIT 1")
    suspend fun getGoalForDate(dateUtc: Long): GoalHistoryEntity?

    @Query("SELECT * FROM goal_history WHERE endDateUtc IS NULL ORDER BY startDateUtc DESC LIMIT 1")
    suspend fun getActiveGoal(): GoalHistoryEntity?

    @Query("SELECT MIN(startDateUtc) FROM goal_history")
    suspend fun getEarliestGoalStartDate(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGoalsSync(goals: List<GoalHistoryEntity>): List<Long>

    @Query("UPDATE goal_history SET endDateUtc = :endDateUtc WHERE id = :id")
    suspend fun closeGoal(id: Int, endDateUtc: Long)

    @Query("UPDATE goal_history SET goalMinutes = :goalMinutes WHERE id = :id")
    suspend fun updateGoalMinutes(id: Int, goalMinutes: Int)

    @Query("DELETE FROM goal_history")
    fun deleteAllGoals()
}