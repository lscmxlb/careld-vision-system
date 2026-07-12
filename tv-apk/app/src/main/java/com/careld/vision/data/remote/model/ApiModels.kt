package com.careld.vision.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper
 */
data class ApiResponse<T>(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: T,
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("traceId")
    val traceId: String?
)

/**
 * Device login request
 */
data class DeviceLoginRequest(
    @SerializedName("storeCode")
    val storeCode: String,
    
    @SerializedName("deviceCode")
    val deviceCode: String,
    
    @SerializedName("appVersion")
    val appVersion: String,
    
    @SerializedName("androidVersion")
    val androidVersion: String,
    
    @SerializedName("screenResolution")
    val screenResolution: String,
    
    @SerializedName("screenSize")
    val screenSize: Double
)

/**
 * Device login response
 */
data class DeviceLoginResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    
    @SerializedName("deviceId")
    val deviceId: Long,
    
    @SerializedName("storeId")
    val storeId: Long,
    
    @SerializedName("storeName")
    val storeName: String,
    
    @SerializedName("calibrationStatus")
    val calibrationStatus: Int,
    
    @SerializedName("calibrationData")
    val calibrationData: CalibrationData?
)

/**
 * Calibration data
 */
data class CalibrationData(
    @SerializedName("pixelPerMm")
    val pixelPerMm: Double,
    
    @SerializedName("screenWidthMm")
    val screenWidthMm: Double,
    
    @SerializedName("screenHeightMm")
    val screenHeightMm: Double,
    
    @SerializedName("screenSizeInch")
    val screenSizeInch: Double,
    
    @SerializedName("calibrationTime")
    val calibrationTime: String
)

/**
 * Token refresh response
 */
data class TokenRefreshResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    
    @SerializedName("expiresIn")
    val expiresIn: Long
)

/**
 * Sync upload request
 */
data class SyncUploadRequest(
    @SerializedName("batchId")
    val batchId: String,
    
    @SerializedName("deviceId")
    val deviceId: Long,
    
    @SerializedName("storeId")
    val storeId: Long,
    
    @SerializedName("records")
    val records: List<VisionRecordUpload>
)

/**
 * Vision record upload data
 */
data class VisionRecordUpload(
    @SerializedName("localId")
    val localId: String,
    
    @SerializedName("childId")
    val childId: String,
    
    @SerializedName("eyeType")
    val eyeType: String,
    
    @SerializedName("visionLevel")
    val visionLevel: String,
    
    @SerializedName("testTime")
    val testTime: String,
    
    @SerializedName("beforeAfter")
    val beforeAfter: String,
    
    @SerializedName("testerName")
    val testerName: String?,
    
    @SerializedName("remark")
    val remark: String?
)

/**
 * Sync upload response
 */
data class SyncUploadResponse(
    @SerializedName("batchId")
    val batchId: String,
    
    @SerializedName("totalCount")
    val totalCount: Int,
    
    @SerializedName("successCount")
    val successCount: Int,
    
    @SerializedName("failCount")
    val failCount: Int,
    
    @SerializedName("results")
    val results: List<SyncResultItem>
)

/**
 * Sync result item
 */
data class SyncResultItem(
    @SerializedName("localId")
    val localId: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("cloudRecordId")
    val cloudRecordId: String?,
    
    @SerializedName("message")
    val message: String?
)

/**
 * Sync download request
 */
data class SyncDownloadRequest(
    @SerializedName("storeId")
    val storeId: Long,
    
    @SerializedName("lastSyncTime")
    val lastSyncTime: Long,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("size")
    val size: Int
)

/**
 * Sync download response
 */
data class SyncDownloadResponse(
    @SerializedName("records")
    val records: List<ChildProfileDownload>,
    
    @SerializedName("hasMore")
    val hasMore: Boolean,
    
    @SerializedName("nextCursor")
    val nextCursor: String?,
    
    @SerializedName("syncTime")
    val syncTime: Long
)

/**
 * Child profile download data
 */
data class ChildProfileDownload(
    @SerializedName("childId")
    val childId: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("birthDate")
    val birthDate: String?,
    
    @SerializedName("gender")
    val gender: Int?,
    
    @SerializedName("medicalHistory")
    val medicalHistory: String?,
    
    @SerializedName("updatedAt")
    val updatedAt: Long
)

/**
 * Sync callback request
 */
data class SyncCallbackRequest(
    @SerializedName("batchId")
    val batchId: String,
    
    @SerializedName("deviceId")
    val deviceId: Long,
    
    @SerializedName("syncType")
    val syncType: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("recordCount")
    val recordCount: Int,
    
    @SerializedName("successCount")
    val successCount: Int,
    
    @SerializedName("failCount")
    val failCount: Int,
    
    @SerializedName("completedAt")
    val completedAt: String
)

/**
 * Sync configuration response
 */
data class SyncConfigResponse(
    @SerializedName("autoSyncInterval")
    val autoSyncInterval: Int,
    
    @SerializedName("maxBatchSize")
    val maxBatchSize: Int,
    
    @SerializedName("retryInterval")
    val retryInterval: List<Int>,
    
    @SerializedName("maxRetryCount")
    val maxRetryCount: Int,
    
    @SerializedName("dataRetentionDays")
    val dataRetentionDays: Int
)

/**
 * Child search result
 */
data class ChildSearchResult(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("childId")
    val childId: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("birthDate")
    val birthDate: String?,
    
    @SerializedName("gender")
    val gender: Int?,
    
    @SerializedName("age")
    val age: Int?
)

/**
 * Calibration update request
 */
data class CalibrationUpdateRequest(
    @SerializedName("calibrationStatus")
    val calibrationStatus: Int,
    
    @SerializedName("calibrationData")
    val calibrationData: CalibrationData
)
