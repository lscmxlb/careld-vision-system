package com.careld.vision.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.careld.vision.R
import com.careld.vision.ui.base.BaseActivity
import com.careld.vision.ui.calibration.CalibrationActivity
import com.careld.vision.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Device login activity
 * 
 * Handles device authentication and store binding.
 */
@AndroidEntryPoint
class DeviceLoginActivity : BaseActivity() {

    override val layoutResId: Int = R.layout.activity_device_login
    
    private val viewModel: DeviceLoginViewModel by viewModels()
    
    private lateinit var etStoreCode: EditText
    private lateinit var etDeviceCode: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun initViews() {
        etStoreCode = findViewById(R.id.etStoreCode)
        etDeviceCode = findViewById(R.id.etDeviceCode)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        
        btnLogin.setOnClickListener { performLogin() }
        
        // Setup focus
        etStoreCode.requestFocus()
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
                viewModel.loginResult.collect { result ->
                    result?.let { handleLoginResult(it) }
                }
            }
        }
    }

    private fun updateUI(state: LoginUiState) {
        btnLogin.isEnabled = !state.isLoading
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        tvError.visibility = if (state.error != null) View.VISIBLE else View.GONE
        tvError.text = state.error
    }

    private fun handleLoginResult(result: LoginResult) {
        when (result) {
            is LoginResult.Success -> {
                if (result.needsCalibration) {
                    navigateToCalibration()
                } else {
                    navigateToMain()
                }
            }
            is LoginResult.Error -> {
                // Error is already shown in UI state
            }
        }
    }

    private fun performLogin() {
        val storeCode = etStoreCode.text.toString().trim()
        val deviceCode = etDeviceCode.text.toString().trim()
        
        if (storeCode.isEmpty()) {
            tvError.text = "请输入门店编码"
            tvError.visibility = View.VISIBLE
            return
        }
        
        if (deviceCode.isEmpty()) {
            tvError.text = "请输入设备码"
            tvError.visibility = View.VISIBLE
            return
        }
        
        viewModel.login(storeCode, deviceCode)
    }

    private fun navigateToCalibration() {
        startActivity(Intent(this, CalibrationActivity::class.java))
        finish()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun setupFocusHandling() {
        // Simple focus chain
        etStoreCode.nextFocusDownId = R.id.etDeviceCode
        etDeviceCode.nextFocusDownId = R.id.btnLogin
        btnLogin.nextFocusUpId = R.id.etDeviceCode
    }
}

/**
 * UI State
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Login result
 */
sealed class LoginResult {
    data class Success(val needsCalibration: Boolean) : LoginResult()
    data class Error(val message: String) : LoginResult()
}
