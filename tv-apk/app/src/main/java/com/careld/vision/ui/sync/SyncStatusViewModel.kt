package com.careld.vision.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.careld.vision.data.local.AppDatabase
import com.careld.vision.data.local.entity.LocalConfigEntity
import com.careld.vision.domain.usecase.SyncDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sync status view model
 */
@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    private val syncDataUseCase: SyncDataUseCase,
    private val database: AppDatabase
) : ViewModel() {

    private val _syncStats = MutableStateFlow(SyncStatsDisplay())
    val syncStats: StateFlow<SyncStatsDisplay> = _syncStats.asStateFlow()
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    init {
        loadStats()
        loadLastSyncTime()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val stats = syncDataUseCase.getSyncStats()
            _syncStats.value = SyncStatsDisplay(
                unsyncedCount = stats.unsyncedCount,
                failedCount = stats.failedCount,
                syncedCount = stats.syncedCount,
                cachedChildren = stats.cachedChildCount
            )
        }
    }

    private fun loadLastSyncTime() {
        viewModelScope.launch {
            val timeStr = database.localConfigDao().getValue(LocalConfigEntity.Keys.LAST_SYNC_TIME)
            _lastSyncTime.value = timeStr?.toLongOrNull()
        }
    }

    fun syncNow() {
        if (_isSyncing.value) return
        
        viewModelScope.launch {
            _isSyncing.value = true
            
            syncDataUseCase.sync().collect { state ->
                when (state) {
                    is SyncDataUseCase.SyncState.Completed -> {
                        _isSyncing.value = false
                        loadStats()
                        loadLastSyncTime()
                    }
                    else -> {
                        // Show progress if needed
                    }
                }
            }
        }
    }

    /**
     * Sync stats display data
     */
    data class SyncStatsDisplay(
        val unsyncedCount: Int = 0,
        val failedCount: Int = 0,
        val syncedCount: Int = 0,
        val cachedChildren: Int = 0
    )
}
