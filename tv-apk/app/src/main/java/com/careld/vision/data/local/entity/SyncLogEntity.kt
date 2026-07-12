package com.careld.vision.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local sync log entity
 * 
 * Tracks synchronization operations for debugging and auditing.
 */
@Entity(
    tableName = "local_sync_log",
    indices = [
        Index(value = ["start_time"])
    ]
)
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "sync_type")
    val syncType: String, // "upload" or "download"

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long?,

    @ColumnInfo(name = "record_count")
    val recordCount: Int = 0,

    @ColumnInfo(name = "success_count")
    val successCount: Int = 0,

    @ColumnInfo(name = "fail_count")
    val failCount: Int = 0,

    @ColumnInfo(name = "status")
    val status: Int = SyncStatus.IN_PROGRESS,

    @ColumnInfo(name = "error_msg")
    val errorMsg: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object SyncStatus {
        const val IN_PROGRESS = 0
        const val SUCCESS = 1
        const val FAILED = 2
    }
}
