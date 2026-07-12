package com.careld.vision.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.careld.vision.R
import com.careld.vision.ui.base.BaseActivity
import com.careld.vision.ui.base.TvFocusHelper
import com.careld.vision.ui.calibration.CalibrationActivity
import com.careld.vision.ui.child.ChildSearchActivity
import com.careld.vision.ui.login.DeviceLoginActivity
import com.careld.vision.ui.settings.SettingsActivity
import com.careld.vision.ui.sync.SyncStatusActivity
import com.careld.vision.ui.vision.VisionTestActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main activity - TV launcher
 * 
 * Main entry point after device login and calibration.
 * Provides navigation to all main features.
 */
@AndroidEntryPoint
class MainActivity : BaseActivity() {

    override val layoutResId: Int = R.layout.activity_main
    
    private val viewModel: MainViewModel by viewModels()
    
    private lateinit var tvStoreName: TextView
    private lateinit var tvDeviceStatus: TextView
    private lateinit var btnVisionTest: Button
    private lateinit var btnChildSearch: Button
    private lateinit var btnSyncStatus: Button
    private lateinit var btnSettings: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        lifecycleScope.launch {
            if (!viewModel.isLoggedIn()) {
                navigateToLogin()
                return@launch
            }
            
            if (!viewModel.isCalibrated()) {
                navigateToCalibration()
                return@launch
            }
        }
    }

    override fun initViews() {
        tvStoreName = findViewById(R.id.tvStoreName)
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus)
        btnVisionTest = findViewById(R.id.btnVisionTest)
        btnChildSearch = findViewById(R.id.btnChildSearch)
        btnSyncStatus = findViewById(R.id.btnSyncStatus)
        btnSettings = findViewById(R.id.btnSettings)
        btnLogout = findViewById(R.id.btnLogout)
        
        // Setup click listeners
        btnVisionTest.setOnClickListener { navigateToVisionTest() }
        btnChildSearch.setOnClickListener { navigateToChildSearch() }
        btnSyncStatus.setOnClickListener { navigateToSyncStatus() }
        btnSettings.setOnClickListener { navigateToSettings() }
        btnLogout.setOnClickListener { performLogout() }
        
        // Setup focus handling
        setupFocusHandling()
    }

    override fun setupFocusHandling() {
        val buttons = listOf(
            btnVisionTest,
            btnChildSearch,
            btnSyncStatus,
            btnSettings,
            btnLogout
        )
        
        TvFocusHelper.setupFocusChain(buttons)
        
        // Request initial focus
        btnVisionTest.requestFocus()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.storeName.collect { name ->
                    tvStoreName.text = name ?: "未知门店"
                }
            }
        }
    }

    private fun updateUI(state: MainUiState) {
        val statusText = buildString {
            append("校准状态: ${if (state.isCalibrated) "已校准" else "未校准"}")
            append(" | ")
            append("未同步: ${state.unsyncedCount}条")
            if (state.isSyncing) {
                append(" | 同步中...")
            }
        }
        tvDeviceStatus.text = statusText
    }

    private fun navigateToVisionTest() {
        startActivity(Intent(this, VisionTestActivity::class.java))
    }

    private fun navigateToChildSearch() {
        startActivity(Intent(this, ChildSearchActivity::class.java))
    }

    private fun navigateToSyncStatus() {
        startActivity(Intent(this, SyncStatusActivity::class.java))
    }

    private fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, DeviceLoginActivity::class.java))
        finish()
    }

    private fun navigateToCalibration() {
        startActivity(Intent(this, CalibrationActivity::class.java))
        finish()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            viewModel.logout()
            navigateToLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStatus()
    }
}

/**
 * UI State
 */
data class MainUiState(
    val isCalibrated: Boolean = false,
    val isSyncing: Boolean = false,
    val unsyncedCount: Int = 0,
    val lastSyncTime: Long? = null
)
