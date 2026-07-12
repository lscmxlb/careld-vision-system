package com.careld.vision.data.local.dao

import androidx.room.*
import com.careld.vision.data.local.entity.VisionRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for vision record operations
 */
@Dao
interface VisionRecordDao {

    @Query("SELECT * FROM local_vision_record ORDER BY test_time DESC")
    fun getAll(): Flow<List<VisionRecordEntity>>

    @Query("SELECT * FROM local_vision_record WHERE sync_status = 0 ORDER BY created_at ASC")
    suspend fun getUnsyncedRecords(): List<VisionRecordEntity>

    @Query("SELECT * FROM local_vision_record WHERE sync_status = 3 ORDER BY retry_count ASC")
    suspend fun getFailedRecords(): List<VisionRecordEntity>

    @Query("SELECT * FROM local_vision_record WHERE child_id = :childId ORDER BY test_time DESC")
    fun getByChildId(childId: String): Flow<List<VisionRecordEntity>>

    @Query("SELECT * FROM local_vision_record WHERE local_id = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): VisionRecordEntity?

    @Query("""
        SELECT * FROM local_vision_record 
        WHERE child_id = :childId 
        AND before_after = :beforeAfter 
        AND eye_type = :eyeType
        ORDER BY test_time DESC 
        LIMIT 1
    """)
    suspend fun getLatestByChildAndType(
        childId: String,
        beforeAfter: String,
        eyeType: String
    ): VisionRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VisionRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<VisionRecordEntity>)

    @Update
    suspend fun update(entity: VisionRecordEntity)

    @Query("""
        UPDATE local_vision_record 
        SET sync_status = :status, 
            cloud_record_id = :cloudRecordId,
            error_msg = :errorMsg,
            last_sync_time = :syncTime
        WHERE local_id = :localId
    """)
    suspend fun updateSyncStatus(
        localId: String,
        status: Int,
        cloudRecordId: String?,
        errorMsg: String?,
        syncTime: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE local_vision_record 
        SET retry_count = retry_count + 1,
            error_msg = :errorMsg
        WHERE local_id = :localId
    """)
    suspend fun incrementRetryCount(localId: String, errorMsg: String? = null)

    @Delete
    suspend fun delete(entity: VisionRecordEntity)

    @Query("SELECT COUNT(*) FROM local_vision_record WHERE sync_status = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("SELECT COUNT(*) FROM local_vision_record WHERE sync_status = 3")
    suspend fun getFailedCount(): Int

    @Query("SELECT COUNT(*) FROM local_vision_record WHERE sync_status = 2")
    suspend fun getSyncedCount(): Int

    @Query("DELETE FROM local_vision_record WHERE sync_status = 2 AND test_time < :beforeTime")
    suspend fun deleteSyncedBefore(beforeTime: Long)

    @Query("DELETE FROM local_vision_record WHERE test_time < :beforeTime")
    suspend fun deleteBefore(beforeTime: Long)
}
