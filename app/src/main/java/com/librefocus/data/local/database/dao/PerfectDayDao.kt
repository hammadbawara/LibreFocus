package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.librefocus.data.local.database.entity.PerfectDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfectDayDao {

    @Query("SELECT * FROM perfect_days WHERE dateUtc = :dateUtc LIMIT 1")
    suspend fun getPerfectDay(dateUtc: Long): PerfectDayEntity?

    @Query(
        """
        SELECT * FROM perfect_days
        WHERE dateUtc >= :startUtc AND dateUtc < :endUtc
        ORDER BY dateUtc ASC
        """
    )
    suspend fun getPerfectDaysInRange(startUtc: Long, endUtc: Long): List<PerfectDayEntity>

    @Query("SELECT * FROM perfect_days ORDER BY dateUtc ASC")
    fun getAllPerfectDays(): Flow<List<PerfectDayEntity>>

    @Query("SELECT * FROM perfect_days ORDER BY dateUtc ASC")
    fun getAllPerfectDaysSync(): List<PerfectDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPerfectDay(entity: PerfectDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertPerfectDaysSync(entities: List<PerfectDayEntity>): List<Long>

    @Query("DELETE FROM perfect_days WHERE dateUtc = :dateUtc")
    suspend fun deletePerfectDay(dateUtc: Long): Int

    @Query("DELETE FROM perfect_days")
    fun deleteAllPerfectDays()
}