package com.careld.vision.ui.sync

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.careld.vision.R
import com.careld.vision.ui.base.BaseActivity
import com.careld.vision.ui.base.TvFocusHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Sync status activity
 * 
 * Shows sync statistics and allows manual sync trigger.
 */
@AndroidEntryPoint
class SyncStatusActivity : BaseActivity() {

    override val layoutResId: Int = R.layout.activity_sync_status
    
    private val viewModel: SyncStatusViewModel by viewModels()
    
    private lateinit var tvUnsyncedCount: TextView
    private lateinit var tvFailedCount: TextView
    private lateinit var tvSyncedCount: TextView
    private lateinit var tvCachedChildren: TextView
    private lateinit var tvLastSync: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSyncNow: Button
    private lateinit var btnViewLogs: Button
    private lateinit var btnBack: Button

    override fun initViews() {
        tvUnsyncedCount = findViewById(R.id.tvUnsyncedCount)
        tvFailedCount = findViewById(R.id.tvFailedCount)
        tvSyncedCount = findViewById(R.id.tvSyncedCount)
        tvCachedChildren = findViewById(R.id.tvCachedChildren)
        tvLastSync = findViewById(R.id.tvLastSync)
        progressBar = findViewById(R.id.progressBar)
        btnSyncNow = findViewById(R.id.btnSyncNow)
        btnViewLogs = findViewById(R.id.btnViewLogs)
        btnBack = findViewById(R.id.btnBack)
        
        btnSyncNow.setOnClickListener { viewModel.syncNow() }
        btnViewLogs.setOnClickListener { navigateToLogs() }
        btnBack.setOnClickListener { finish() }
        
        setupFocusHandling()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.syncStats.collect { stats ->
                    updateStats(stats)
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isSyncing.collect { isSyncing ->
                    updateSyncingState(isSyncing)
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lastSyncTime.collect { time ->
                    tvLastSync.text = time?.let { 
                        com.careld.vision.core.utils.DateUtils.getRelativeTime(it)
                    } ?: "从未同步"
                }
            }
        }
    }

    private fun updateStats(stats: SyncStatusViewModel.SyncStatsDisplay) {
        tvUnsyncedCount.text = "未同步: ${stats.unsyncedCount}条"
        tvFailedCount.text = "同步失败: ${stats.failedCount}条"
        tvSyncedCount.text = "已同步: ${stats.syncedCount}条"
        tvCachedChildren.text = "缓存儿童: ${stats.cachedChildren}人"
    }

    private fun updateSyncingState(isSyncing: Boolean) {
        progressBar.visibility = if (isSyncing) View.VISIBLE else View.GONE
        btnSyncNow.isEnabled = !isSyncing
        btnSyncNow.text = if (isSyncing) "同步中..." else "立即同步"
    }

    private fun navigateToLogs() {
        // Would navigate to sync logs activity
        // startActivity(Intent(this, SyncLogActivity::class.java))
    }

    override fun setupFocusHandling() {
        val buttons = listOf(btnSyncNow, btnViewLogs, btnBack)
        TvFocusHelper.setupFocusChain(buttons)
        btnSyncNow.requestFocus()
    }
}
