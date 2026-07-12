package com.careld.vision.domain.usecase

import com.careld.vision.data.local.AppDatabase
import com.careld.vision.data.local.entity.LocalConfigEntity
import com.careld.vision.data.local.entity.SyncLogEntity
import com.careld.vision.data.local.entity.VisionRecordEntity
import com.careld.vision.data.remote.SyncApi
import com.careld.vision.data.remote.model.*
import com.careld.vision.domain.model.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync data use case
 * 
 * Handles synchronization between local database and cloud.
 */
@Singleton
class SyncDataUseCase @Inject constructor(
    private val database: AppDatabase,
    private val syncApi: SyncApi
) {

    companion object {
        private const val BATCH_SIZE = 100
        private const val TAG = "SyncDataUseCase"
    }

    /**
     * Execute full sync (upload + download)
     * @return Flow of sync progress and result
     */
    fun sync(): Flow<SyncState> = flow {
        emit(SyncState.Starting)
        
        val startTime = System.currentTimeMillis()
        val logId = createSyncLog(SyncLogEntity.SyncStatus.IN_PROGRESS)
        
        try {
            // 1. Upload local records
            emit(SyncState.Uploading)
            val uploadResult = uploadVisionRecords()
            
            // 2. Download child profiles
            emit(SyncState.Downloading)
            val downloadResult = downloadChildProfiles()
            
            // 3. Update sync log
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            updateSyncLog(
                logId = logId,
                status = SyncLogEntity.SyncStatus.SUCCESS,
                endTime = endTime,
                recordCount = uploadResult.totalCount + downloadResult.count,
                successCount = uploadResult.successCount + downloadResult.count,
                failCount = uploadResult.failCount
            )
            
            // 4. Update last sync time
            updateLastSyncTime(endTime)
            
            emit(SyncState.Completed(
                SyncResult.Success(
                    uploadedCount = uploadResult.successCount,
                    downloadedCount = downloadResult.count,
                    durationMs = duration
                )
            ))
            
        } catch (e: Exception) {
            Timber.e(e, "Sync failed")
            
            val endTime = System.currentTimeMillis()
            updateSyncLog(
                logId = logId,
                status = SyncLogEntity.SyncStatus.FAILED,
                endTime = endTime,
                recordCount = 0,
                successCount = 0,
                failCount = 0,
                errorMsg = e.message
            )
            
            emit(SyncState.Completed(
                SyncResult.Error(
                    message = e.message ?: "同步失败",
                    isNetworkError = isNetworkError(e)
                )
            ))
        }
    }

    /**
     * Upload vision records to cloud
     */
    private suspend fun uploadVisionRecords(): UploadResult = withContext(Dispatchers.IO) {
        val records = database.visionRecordDao().getUnsyncedRecords()
        
        if (records.isEmpty()) {
            return UploadResult(0, 0, 0)
        }
        
        // Mark records as syncing
        records.forEach { record ->
            database.visionRecordDao().updateSyncStatus(
                localId = record.localId,
                status = VisionRecordEntity.SYNCING,
                cloudRecordId = null,
                errorMsg = null
            )
        }
        
        val batchId = generateBatchId()
        val storeId = getStoreId()
        val deviceId = getDeviceId()
        
        val uploadRecords = records.map { it.toUploadRecord() }
        
        val request = SyncUploadRequest(
            batchId = batchId,
            deviceId = deviceId,
            storeId = storeId,
            records = uploadRecords
        )
        
        val response = syncApi.uploadVisionRecords(request)
        
        if (response.isSuccessful) {
            val result = response.body()?.data
                ?: throw Exception("Empty response")
            
            // Update local records based on result
            result.results.forEach { item ->
                val status = if (item.status == "success") {
                    VisionRecordEntity.SYNCED
                } else {
                    VisionRecordEntity.FAILED
                }
                
                database.visionRecordDao().updateSyncStatus(
                    localId = item.localId,
                    status = status,
                    cloudRecordId = item.cloudRecordId,
                    errorMsg = item.message
                )
            }
            
            UploadResult(
                totalCount = result.totalCount,
                successCount = result.successCount,
                failCount = result.failCount
            )
        } else {
            // Mark all as failed
            records.forEach { record ->
                database.visionRecordDao().updateSyncStatus(
                    localId = record.localId,
                    status = VisionRecordEntity.FAILED,
                    cloudRecordId = null,
                    errorMsg = "Upload failed: ${response.code()}"
                )
            }
            throw Exception("Upload failed: ${response.code()}")
        }
    }

    /**
     * Download child profiles from cloud
     */
    private suspend fun downloadChildProfiles(): DownloadResult = withContext(Dispatchers.IO) {
        val storeId = getStoreId()
        val lastSyncTime = getLastSyncTime()
        
        val request = SyncDownloadRequest(
            storeId = storeId,
            lastSyncTime = lastSyncTime,
            page = 1,
            size = BATCH_SIZE
        )
        
        val response = syncApi.downloadChildProfiles(request)
        
        if (response.isSuccessful) {
            val result = response.body()?.data
                ?: throw Exception("Empty response")
            
            // Save to local database
            val entities = result.records.map { it.toEntity() }
            database.childProfileDao().insertAll(entities)
            
            DownloadResult(
                count = result.records.size,
                hasMore = result.hasMore
            )
        } else {
            throw Exception("Download failed: ${response.code()}")
        }
    }

    /**
     * Get sync statistics
     */
    suspend fun getSyncStats(): SyncStats = withContext(Dispatchers.IO) {
        SyncStats(
            unsyncedCount = database.visionRecordDao().getUnsyncedCount(),
            failedCount = database.visionRecordDao().getFailedCount(),
            syncedCount = database.visionRecordDao().getSyncedCount(),
            totalLocalRecords = database.visionRecordDao().getUnsyncedCount() + 
                               database.visionRecordDao().getSyncedCount() +
                               database.visionRecordDao().getFailedCount(),
            cachedChildCount = database.childProfileDao().getCount()
        )
    }

    private suspend fun createSyncLog(status: Int): Long {
        val log = SyncLogEntity(
            syncType = "upload",
            startTime = System.currentTimeMillis(),
            status = status,
            errorMsg = null
        )
        return database.syncLogDao().insert(log)
    }

    private suspend fun updateSyncLog(
        logId: Long,
        status: Int,
        endTime: Long,
        recordCount: Int,
        successCount: Int,
        failCount: Int,
        errorMsg: String? = null
    ) {
        database.syncLogDao().updateStatus(
            id = logId,
            endTime = endTime,
            status = status,
            recordCount = recordCount,
            successCount = successCount,
            failCount = failCount,
            errorMsg = errorMsg
        )
    }

    private suspend fun updateLastSyncTime(time: Long) {
        database.localConfigDao().updateValue(
            LocalConfigEntity.Keys.LAST_SYNC_TIME,
            time.toString()
        )
    }

    private suspend fun getStoreId(): Long {
        return database.localConfigDao().getValue(LocalConfigEntity.Keys.STORE_ID)?.toLongOrNull() ?: 0L
    }

    private suspend fun getDeviceId(): Long {
        return database.localConfigDao().getValue(LocalConfigEntity.Keys.DEVICE_ID)?.toLongOrNull() ?: 0L
    }

    private suspend fun getLastSyncTime(): Long {
        return database.localConfigDao().getValue(LocalConfigEntity.Keys.LAST_SYNC_TIME)?.toLongOrNull() ?: 0L
    }

    private fun generateBatchId(): String {
        return "batch_${UUID.randomUUID().toString().replace("-", "").take(16)}_${System.currentTimeMillis()}"
    }

    private fun isNetworkError(e: Exception): Boolean {
        return e is java.net.UnknownHostException ||
               e is java.net.SocketTimeoutException ||
               e is java.net.ConnectException
    }

    // Extension functions
    private fun VisionRecordEntity.toUploadRecord(): VisionRecordUpload {
        return VisionRecordUpload(
            localId = localId,
            childId = childId,
            eyeType = eyeType,
            visionLevel = visionLevel,
            testTime = java.time.Instant.ofEpochMilli(testTime).toString(),
            beforeAfter = beforeAfter,
            testerName = testerName,
            remark = remark
        )
    }

    private fun ChildProfileDownload.toEntity(): com.careld.vision.data.local.entity.ChildProfileEntity {
        return com.careld.vision.data.local.entity.ChildProfileEntity(
            childId = childId,
            name = name,
            phone = phone,
            birthDate = birthDate,
            gender = gender,
            medicalHistory = medicalHistory,
            syncTime = System.currentTimeMillis(),
            cloudUpdatedAt = updatedAt
        )
    }

    // Data classes
    data class UploadResult(
        val totalCount: Int,
        val successCount: Int,
        val failCount: Int
    )

    data class DownloadResult(
        val count: Int,
        val hasMore: Boolean
    )

    data class SyncStats(
        val unsyncedCount: Int,
        val failedCount: Int,
        val syncedCount: Int,
        val totalLocalRecords: Int,
        val cachedChildCount: Int
    )

    // Sync state sealed class
    sealed class SyncState {
        object Starting : SyncState()
        object Uploading : SyncState()
        object Downloading : SyncState()
        data class Completed(val result: SyncResult) : SyncState()
    }
}
