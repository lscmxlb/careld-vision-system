package com.careld.vision.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local configuration entity
 * 
 * Stores app configuration and device binding info.
 */
@Entity(
    tableName = "local_config",
    indices = [
        Index(value = ["config_key"], unique = true)
    ]
)
data class LocalConfigEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "config_key")
    val configKey: String,

    @ColumnInfo(name = "config_value")
    val configValue: String?,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object Keys {
        const val STORE_ID = "store_id"
        const val DEVICE_CODE = "device_code"
        const val DEVICE_ID = "device_id"
        const val LAST_SYNC_TIME = "last_sync_time"
        const val CALIBRATION_DATA = "calibration_data"
        const val APP_VERSION = "app_version"
        const val VISION_TEST_DISTANCE = "vision_test_distance"
        const val AUTO_SYNC_INTERVAL = "auto_sync_interval"
        const val STORE_NAME = "store_name"
        const val ACCESS_TOKEN = "access_token"
    }
}
