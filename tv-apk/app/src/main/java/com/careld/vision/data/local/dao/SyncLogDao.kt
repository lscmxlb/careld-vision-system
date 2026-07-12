package com.careld.vision.data.local.dao

import androidx.room.*
import com.careld.vision.data.local.entity.SyncLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for sync log operations
 */
@Dao
interface SyncLogDao {

    @Query("SELECT * FROM local_sync_log ORDER BY start_time DESC")
    fun getAll(): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM local_sync_log ORDER BY start_time DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SyncLogEntity>

    @Query("SELECT * FROM local_sync_log WHERE sync_type = :syncType ORDER BY start_time DESC")
    fun getByType(syncType: String): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM local_sync_log WHERE status = 0 ORDER BY start_time DESC LIMIT 1")
    suspend fun getLatestInProgress(): SyncLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SyncLogEntity): Long

    @Update
    suspend fun update(entity: SyncLogEntity)

    @Query("""
        UPDATE local_sync_log 
        SET end_time = :endTime, 
            status = :status,
            record_count = :recordCount,
            success_count = :successCount,
            fail_count = :failCount,
            error_msg = :errorMsg
        WHERE id = :id
    """)
    suspend fun updateStatus(
        id: Long,
        endTime: Long,
        status: Int,
        recordCount: Int,
        successCount: Int,
        failCount: Int,
        errorMsg: String?
    )

    @Delete
    suspend fun delete(entity: SyncLogEntity)

    @Query("DELETE FROM local_sync_log WHERE start_time < :beforeTime")
    suspend fun deleteBefore(beforeTime: Long)

    @Query("SELECT COUNT(*) FROM local_sync_log")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM local_sync_log WHERE status = 1")
    suspend fun getSuccessCount(): Int

    @Query("SELECT COUNT(*) FROM local_sync_log WHERE status = 2")
    suspend fun getFailedCount(): Int
}
