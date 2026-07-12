package com.careld.vision.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.careld.vision.data.local.AppDatabase
import com.careld.vision.data.local.entity.LocalConfigEntity
import com.careld.vision.domain.usecase.CalibrationUseCase
import com.careld.vision.domain.usecase.SyncDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main view model
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val database: AppDatabase,
    private val calibrationUseCase: CalibrationUseCase,
    private val syncDataUseCase: SyncDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _storeName = MutableStateFlow<String?>(null)
    val storeName: StateFlow<String?> = _storeName.asStateFlow()

    init {
        loadStoreInfo()
        refreshStatus()
    }

    suspend fun isLoggedIn(): Boolean {
        val token = database.localConfigDao().getValue(LocalConfigEntity.Keys.ACCESS_TOKEN)
        return !token.isNullOrBlank()
    }

    suspend fun isCalibrated(): Boolean {
        return calibrationUseCase.isCalibrated()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val isCalibrated = calibrationUseCase.isCalibrated()
            val stats = syncDataUseCase.getSyncStats()
            
            _uiState.value = _uiState.value.copy(
                isCalibrated = isCalibrated,
                unsyncedCount = stats.unsyncedCount
            )
        }
    }

    private fun loadStoreInfo() {
        viewModelScope.launch {
            val name = database.localConfigDao().getValue(LocalConfigEntity.Keys.STORE_NAME)
            _storeName.value = name
        }
    }

    suspend fun logout() {
        // Clear all local data
        database.localConfigDao().deleteAll()
        database.childProfileDao().deleteAll()
        database.visionRecordDao().deleteAll()
    }
}
