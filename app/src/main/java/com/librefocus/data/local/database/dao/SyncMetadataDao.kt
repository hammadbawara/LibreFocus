package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.librefocus.data.local.database.entity.SyncMetadataEntity

/**
 * Data Access Object for sync metadata.
 */
@Dao
interface SyncMetadataDao {
    
    @Query("SELECT * FROM sync_metadata WHERE key = :key LIMIT 1")
    suspend fun getMetadata(key: String): SyncMetadataEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: SyncMetadataEntity)
    
    @Query("DELETE FROM sync_metadata WHERE key = :key")
    suspend fun deleteMetadata(key: String)
    
    // Synchronous methods for backup/restore operations
    @Query("SELECT * FROM sync_metadata")
    fun getAllMetadata(): kotlinx.coroutines.flow.Flow<List<SyncMetadataEntity>>
    
    @Query("SELECT * FROM sync_metadata")
    fun getAllMetadataSync(): List<SyncMetadataEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMetadataSync(metadata: List<SyncMetadataEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSync(metadata: SyncMetadataEntity)
    
    @Query("DELETE FROM sync_metadata")
    fun deleteAllMetadata()
}
