package com.careld.vision.domain.model

/**
 * Sync result domain model
 */
sealed class SyncResult {
    data class Success(
        val uploadedCount: Int,
        val downloadedCount: Int,
        val durationMs: Long
    ) : SyncResult()

    data class Error(
        val message: String,
        val isNetworkError: Boolean = false
    ) : SyncResult()

    data class Partial(
        val uploadedCount: Int,
        val failedUploads: Int,
        val downloadedCount: Int,
        val message: String
    ) : SyncResult()
}

/**
 * Sync log domain model
 */
data class SyncLog(
    val id: Long,
    val syncType: SyncType,
    val startTime: Long,
    val endTime: Long?,
    val recordCount: Int,
    val successCount: Int,
    val failCount: Int,
    val status: SyncStatus,
    val errorMsg: String?
) {
    enum class SyncType {
        UPLOAD, DOWNLOAD
    }

    enum class SyncStatus {
        IN_PROGRESS, SUCCESS, FAILED
    }

    companion object {
        fun fromSyncTypeString(type: String): SyncType {
            return when (type.lowercase()) {
                "upload" -> SyncType.UPLOAD
                "download" -> SyncType.DOWNLOAD
                else -> SyncType.UPLOAD
            }
        }

        fun toSyncTypeString(type: SyncType): String {
            return when (type) {
                SyncType.UPLOAD -> "upload"
                SyncType.DOWNLOAD -> "download"
            }
        }

        fun fromSyncStatusCode(code: Int): SyncStatus {
            return when (code) {
                0 -> SyncStatus.IN_PROGRESS
                1 -> SyncStatus.SUCCESS
                2 -> SyncStatus.FAILED
                else -> SyncStatus.IN_PROGRESS
            }
        }

        fun toSyncStatusCode(status: SyncStatus): Int {
            return when (status) {
                SyncStatus.IN_PROGRESS -> 0
                SyncStatus.SUCCESS -> 1
                SyncStatus.FAILED -> 2
            }
        }
    }

    fun getSyncTypeDisplay(): String {
        return when (syncType) {
            SyncType.UPLOAD -> "上传"
            SyncType.DOWNLOAD -> "下载"
        }
    }

    fun getStatusDisplay(): String {
        return when (status) {
            SyncStatus.IN_PROGRESS -> "进行中"
            SyncStatus.SUCCESS -> "成功"
            SyncStatus.FAILED -> "失败"
        }
    }

    fun getDurationMs(): Long {
        return if (endTime != null) {
            endTime - startTime
        } else {
            System.currentTimeMillis() - startTime
        }
    }
}
