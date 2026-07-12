package com.careld.vision.data.local.dao

import androidx.room.*
import com.careld.vision.data.local.entity.ChildProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for child profile operations
 */
@Dao
interface ChildProfileDao {

    @Query("SELECT * FROM local_child_profile ORDER BY name ASC")
    fun getAll(): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM local_child_profile WHERE name LIKE '%' || :keyword || '%' ORDER BY name ASC")
    fun searchByName(keyword: String): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM local_child_profile WHERE child_id = :childId LIMIT 1")
    suspend fun getByChildId(childId: String): ChildProfileEntity?

    @Query("SELECT * FROM local_child_profile WHERE sync_time < :syncTime")
    suspend fun getOutdated(syncTime: Long): List<ChildProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChildProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ChildProfileEntity>)

    @Update
    suspend fun update(entity: ChildProfileEntity)

    @Delete
    suspend fun delete(entity: ChildProfileEntity)

    @Query("DELETE FROM local_child_profile WHERE child_id = :childId")
    suspend fun deleteByChildId(childId: String)

    @Query("SELECT COUNT(*) FROM local_child_profile")
    suspend fun getCount(): Int

    @Query("SELECT MAX(sync_time) FROM local_child_profile")
    suspend fun getLastSyncTime(): Long?

    @Query("DELETE FROM local_child_profile")
    suspend fun deleteAll()
}
