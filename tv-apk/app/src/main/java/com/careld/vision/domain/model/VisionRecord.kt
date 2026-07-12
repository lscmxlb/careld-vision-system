package com.careld.vision.domain.model

/**
 * Vision record domain model
 */
data class VisionRecord(
    val id: Long,
    val localId: String,
    val childId: String,
    val eyeType: EyeType,
    val visionLevel: String,
    val testTime: Long,
    val beforeAfter: TestType,
    val testerName: String?,
    val remark: String?,
    val syncStatus: SyncStatus,
    val retryCount: Int,
    val cloudRecordId: String?
) {
    enum class EyeType {
        LEFT, RIGHT
    }

    enum class TestType {
        BEFORE, AFTER
    }

    enum class SyncStatus {
        UNSYNCED, SYNCING, SYNCED, FAILED
    }

    companion object {
        fun fromEyeTypeString(type: String): EyeType {
            return when (type.lowercase()) {
                "left" -> EyeType.LEFT
                "right" -> EyeType.RIGHT
                else -> EyeType.LEFT
            }
        }

        fun toEyeTypeString(type: EyeType): String {
            return when (type) {
                EyeType.LEFT -> "left"
                EyeType.RIGHT -> "right"
            }
        }

        fun fromTestTypeString(type: String): TestType {
            return when (type.lowercase()) {
                "before" -> TestType.BEFORE
                "after" -> TestType.AFTER
                else -> TestType.BEFORE
            }
        }

        fun toTestTypeString(type: TestType): String {
            return when (type) {
                TestType.BEFORE -> "before"
                TestType.AFTER -> "after"
            }
        }

        fun fromSyncStatusCode(code: Int): SyncStatus {
            return when (code) {
                0 -> SyncStatus.UNSYNCED
                1 -> SyncStatus.SYNCING
                2 -> SyncStatus.SYNCED
                3 -> SyncStatus.FAILED
                else -> SyncStatus.UNSYNCED
            }
        }

        fun toSyncStatusCode(status: SyncStatus): Int {
            return when (status) {
                SyncStatus.UNSYNCED -> 0
                SyncStatus.SYNCING -> 1
                SyncStatus.SYNCED -> 2
                SyncStatus.FAILED -> 3
            }
        }
    }

    fun getEyeTypeDisplay(): String {
        return when (eyeType) {
            EyeType.LEFT -> "左眼"
            EyeType.RIGHT -> "右眼"
        }
    }

    fun getTestTypeDisplay(): String {
        return when (beforeAfter) {
            TestType.BEFORE -> "养护前"
            TestType.AFTER -> "养护后"
        }
    }

    fun getSyncStatusDisplay(): String {
        return when (syncStatus) {
            SyncStatus.UNSYNCED -> "未同步"
            SyncStatus.SYNCING -> "同步中"
            SyncStatus.SYNCED -> "已同步"
            SyncStatus.FAILED -> "同步失败"
        }
    }
}
