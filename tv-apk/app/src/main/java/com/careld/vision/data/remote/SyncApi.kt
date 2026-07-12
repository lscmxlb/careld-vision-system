package com.careld.vision.data.remote

import com.careld.vision.data.remote.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Synchronization API interface
 */
interface SyncApi {

    /**
     * Upload vision records
     * POST /api/v1/sync/upload
     */
    @POST("sync/upload")
    suspend fun uploadVisionRecords(
        @Body request: SyncUploadRequest
    ): Response<ApiResponse<SyncUploadResponse>>

    /**
     * Download child profiles
     * POST /api/v1/sync/download/children
     */
    @POST("sync/download/children")
    suspend fun downloadChildProfiles(
        @Body request: SyncDownloadRequest
    ): Response<ApiResponse<SyncDownloadResponse>>

    /**
     * Sync callback
     * POST /api/v1/sync/callback
     */
    @POST("sync/callback")
    suspend fun syncCallback(
        @Body request: SyncCallbackRequest
    ): Response<ApiResponse<Unit>>

    /**
     * Get sync configuration
     * GET /api/v1/sync/config
     */
    @GET("sync/config")
    suspend fun getSyncConfig(): Response<ApiResponse<SyncConfigResponse>>

    /**
     * Search children for TV
     * GET /api/v1/children/search
     */
    @GET("children/search")
    suspend fun searchChildren(
        @Query("keyword") keyword: String,
        @Query("storeId") storeId: Long
    ): Response<ApiResponse<List<ChildSearchResult>>>

    /**
     * Update device calibration
     * PUT /api/v1/devices/{id}/calibration
     */
    @POST("devices/{id}/calibration")
    suspend fun updateCalibration(
        @retrofit2.http.Path("id") deviceId: Long,
        @Body request: CalibrationUpdateRequest
    ): Response<ApiResponse<Unit>>
}
