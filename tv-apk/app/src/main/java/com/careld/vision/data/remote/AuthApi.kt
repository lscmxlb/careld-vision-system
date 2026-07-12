package com.careld.vision.data.remote

import com.careld.vision.data.remote.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Authentication API interface
 */
interface AuthApi {

    /**
     * Device login
     * POST /api/v1/auth/device-login
     */
    @POST("auth/device-login")
    suspend fun deviceLogin(
        @Body request: DeviceLoginRequest
    ): Response<ApiResponse<DeviceLoginResponse>>

    /**
     * Refresh token
     * POST /api/v1/auth/refresh
     */
    @POST("auth/refresh")
    suspend fun refreshToken(): Response<ApiResponse<TokenRefreshResponse>>

    /**
     * Device logout
     * POST /api/v1/auth/logout
     */
    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
}
