package com.librefocus.data.local.database.dao

import androidx.room.*
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
}
