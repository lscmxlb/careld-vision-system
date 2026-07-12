package com.careld.vision.data.local.dao

import androidx.room.*
import com.careld.vision.data.local.entity.LocalConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for local configuration operations
 */
@Dao
interface LocalConfigDao {

    @Query("SELECT * FROM local_config WHERE config_key = :key LIMIT 1")
    suspend fun getByKey(key: String): LocalConfigEntity?

    @Query("SELECT config_value FROM local_config WHERE config_key = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT * FROM local_config ORDER BY config_key ASC")
    fun getAll(): Flow<List<LocalConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LocalConfigEntity>)

    @Query("UPDATE local_config SET config_value = :value, updated_at = :timestamp WHERE config_key = :key")
    suspend fun updateValue(key: String, value: String?, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(entity: LocalConfigEntity)

    @Query("DELETE FROM local_config WHERE config_key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM local_config")
    suspend fun deleteAll()
}
