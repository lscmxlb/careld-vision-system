package com.careld.vision.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local vision record entity
 * 
 * Stores vision test records locally before syncing to cloud.
 */
@Entity(
    tableName = "local_vision_record",
    indices = [
        Index(value = ["local_id"], unique = true),
        Index(value = ["sync_status"]),
        Index(value = ["child_id"]),
        Index(value = ["test_time"])
    ]
)
data class VisionRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "local_id")
    val localId: String, // UUID generated locally

    @ColumnInfo(name = "child_id")
    val childId: String,

    @ColumnInfo(name = "eye_type")
    val eyeType: String, // "left" or "right"

    @ColumnInfo(name = "vision_level")
    val visionLevel: String, // e.g., "4.8", "5.0"

    @ColumnInfo(name = "test_time")
    val testTime: Long,

    @ColumnInfo(name = "before_after")
    val beforeAfter: String, // "before" or "after"

    @ColumnInfo(name = "tester_name")
    val testerName: String?,

    @ColumnInfo(name = "remark")
    val remark: String?,

    // Sync related fields
    @ColumnInfo(name = "sync_status")
    val syncStatus: Int = SyncStatus.UNSYNCED,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "last_sync_time")
    val lastSyncTime: Long?,

    @ColumnInfo(name = "error_msg")
    val errorMsg: String?,

    @ColumnInfo(name = "cloud_record_id")
    val cloudRecordId: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object SyncStatus {
        const val UNSYNCED = 0
        const val SYNCING = 1
        const val SYNCED = 2
        const val FAILED = 3
    }
}
