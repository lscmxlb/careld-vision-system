package com.careld.vision.ui.vision

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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Vision test activity
 * 
 * Main vision testing screen with electronic vision chart.
 */
@AndroidEntryPoint
class VisionTestActivity : BaseActivity() {

    override val layoutResId: Int = R.layout.activity_vision_test
    
    private val viewModel: VisionTestViewModel by viewModels()
    
    private lateinit var visionChartView: VisionChartView
    private lateinit var tvChildInfo: TextView
    private lateinit var tvTestInfo: TextView
    private lateinit var btnLeftEye: Button
    private lateinit var btnRightEye: Button
    private lateinit var btnBefore: Button
    private lateinit var btnAfter: Button
    private lateinit var btnStart: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    override fun initViews() {
        visionChartView = findViewById(R.id.visionChartView)
        tvChildInfo = findViewById(R.id.tvChildInfo)
        tvTestInfo = findViewById(R.id.tvTestInfo)
        btnLeftEye = findViewById(R.id.btnLeftEye)
        btnRightEye = findViewById(R.id.btnRightEye)
        btnBefore = findViewById(R.id.btnBefore)
        btnAfter = findViewById(R.id.btnAfter)
        btnStart = findViewById(R.id.btnStart)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        
        // Setup vision chart callbacks
        visionChartView.onVisionResult = { visionLevel ->
            viewModel.onVisionResult(visionLevel)
        }
        
        // Setup buttons
        btnLeftEye.setOnClickListener { viewModel.selectEye(VisionTestViewModel.Eye.LEFT) }
        btnRightEye.setOnClickListener { viewModel.selectEye(VisionTestViewModel.Eye.RIGHT) }
        btnBefore.setOnClickListener { viewModel.selectType(VisionTestViewModel.TestType.BEFORE) }
        btnAfter.setOnClickListener { viewModel.selectType(VisionTestViewModel.TestType.AFTER) }
        btnStart.setOnClickListener { startTest() }
        btnSave.setOnClickListener { saveResult() }
        btnCancel.setOnClickListener { finish() }
        
        // Initial focus
        btnStart.requestFocus()
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
                viewModel.calibrationData.collect { data ->
                    data?.let { visionChartView.setCalibrationData(it) }
                }
            }
        }
    }

    private fun updateUI(state: VisionTestUiState) {
        // Update eye selection
        btnLeftEye.isSelected = state.selectedEye == VisionTestViewModel.Eye.LEFT
        btnRightEye.isSelected = state.selectedEye == VisionTestViewModel.Eye.RIGHT
        
        // Update type selection
        btnBefore.isSelected = state.selectedType == VisionTestViewModel.TestType.BEFORE
        btnAfter.isSelected = state.selectedType == VisionTestViewModel.TestType.AFTER
        
        // Update test info
        val infoText = buildString {
            append("检测眼: ${state.selectedEye?.displayName ?: "未选择"}")
            append(" | ")
            append("检测类型: ${state.selectedType?.displayName ?: "未选择"}")
            if (state.currentVisionLevel != null) {
                append(" | ")
                append("当前视力: ${state.currentVisionLevel}")
            }
        }
        tvTestInfo.text = infoText
        
        // Update buttons
        btnStart.isEnabled = state.canStartTest
        btnSave.isEnabled = state.testResult != null
    }

    private fun startTest() {
        visionChartView.startTest()
    }

    private fun saveResult() {
        viewModel.saveResult()
        finish()
    }

    override fun setupFocusHandling() {
        val topButtons = listOf(btnLeftEye, btnRightEye, btnBefore, btnAfter)
        val bottomButtons = listOf(btnStart, btnSave, btnCancel)
        
        TvFocusHelper.setupFocusChain(topButtons)
        TvFocusHelper.setupFocusChain(bottomButtons)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        // Let vision chart view handle its own key events
        if (visionChartView.hasFocus()) {
            return visionChartView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }
}

/**
 * UI State
 */
data class VisionTestUiState(
    val selectedEye: VisionTestViewModel.Eye? = null,
    val selectedType: VisionTestViewModel.TestType? = null,
    val currentVisionLevel: String? = null,
    val testResult: String? = null,
    val canStartTest: Boolean = false
)
