package com.careld.vision.ui.login

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.careld.vision.data.local.AppDatabase
import com.careld.vision.data.local.entity.LocalConfigEntity
import com.careld.vision.data.remote.AuthApi
import com.careld.vision.data.remote.model.DeviceLoginRequest
import com.careld.vision.domain.usecase.CalibrationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Device login view model
 */
@HiltViewModel
class DeviceLoginViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val database: AppDatabase,
    private val calibrationUseCase: CalibrationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    private val _loginResult = MutableSharedFlow<LoginResult?>()
    val loginResult: SharedFlow<LoginResult?> = _loginResult.asSharedFlow()

    fun login(storeCode: String, deviceCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get device info
                val screenResolution = getScreenResolution()
                val screenSize = 55.0 // Default, will be calibrated
                
                val request = DeviceLoginRequest(
                    storeCode = storeCode,
                    deviceCode = deviceCode,
                    appVersion = "1.0.0",
                    androidVersion = Build.VERSION.RELEASE,
                    screenResolution = screenResolution,
                    screenSize = screenSize
                )
                
                val response = authApi.deviceLogin(request)
                
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    
                    if (data != null) {
                        // Save login data
                        saveLoginData(data, storeCode)
                        
                        val needsCalibration = data.calibrationStatus == 0
                        _loginResult.emit(LoginResult.Success(needsCalibration))
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "登录失败：服务器返回数据为空"
                        )
                        _loginResult.emit(LoginResult.Error("Empty response"))
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "门店编码或设备码错误"
                        403 -> "设备已被禁用"
                        404 -> "门店不存在"
                        else -> "登录失败：${response.message()}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                    _loginResult.emit(LoginResult.Error(errorMsg))
                }
            } catch (e: Exception) {
                Timber.e(e, "Login failed")
                val errorMsg = "网络错误，请检查网络连接"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMsg
                )
                _loginResult.emit(LoginResult.Error(errorMsg))
            }
        }
    }

    private suspend fun saveLoginData(
        data: com.careld.vision.data.remote.model.DeviceLoginResponse,
        storeCode: String
    ) {
        // Save to local config
        database.localConfigDao().insertAll(
            listOf(
                LocalConfigEntity(
                    configKey = LocalConfigEntity.Keys.ACCESS_TOKEN,
                    configValue = data.accessToken
                ),
                LocalConfigEntity(
                    configKey = LocalConfigEntity.Keys.STORE_ID,
                    configValue = data.storeId.toString()
                ),
                LocalConfigEntity(
                    configKey = LocalConfigEntity.Keys.DEVICE_ID,
                    configValue = data.deviceId.toString()
                ),
                LocalConfigEntity(
                    configKey = LocalConfigEntity.Keys.STORE_NAME,
                    configValue = data.storeName
                ),
                LocalConfigEntity(
                    configKey = LocalConfigEntity.Keys.DEVICE_CODE,
                    configValue = storeCode
                )
            )
        )
        
        // Save calibration data if exists
        if (data.calibrationData != null) {
            val calData = com.careld.vision.domain.model.CalibrationData(
                pixelPerMm = data.calibrationData.pixelPerMm,
                screenWidthMm = data.calibrationData.screenWidthMm,
                screenHeightMm = data.calibrationData.screenHeightMm,
                screenSizeInch = data.calibrationData.screenSizeInch,
                calibrationTime = System.currentTimeMillis(),
                isCalibrated = true
            )
            calibrationUseCase.saveCalibrationData(calData)
        }
    }

    private fun getScreenResolution(): String {
        // This would normally get actual screen resolution
        // For now, return a default
        return "1920x1080"
    }
}
