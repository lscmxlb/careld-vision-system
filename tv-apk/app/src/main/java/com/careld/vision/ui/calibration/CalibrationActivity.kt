package com.careld.vision.ui.calibration

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.careld.vision.R
import com.careld.vision.ui.base.BaseActivity
import com.careld.vision.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Screen calibration activity
 * 
 * Calibrates screen using credit card method for accurate vision testing.
 */
@AndroidEntryPoint
class CalibrationActivity : BaseActivity() {

    override val layoutResId: Int = R.layout.activity_calibration
    
    private val viewModel: CalibrationViewModel by viewModels()
    
    private lateinit var calibrationCard: View
    private lateinit var seekBarWidth: SeekBar
    private lateinit var seekBarHeight: SeekBar
    private lateinit var btnConfirm: Button
    private lateinit var btnSkip: Button
    private lateinit var tvInstructions: TextView
    private lateinit var tvCalibrationInfo: TextView

    override fun initViews() {
        calibrationCard = findViewById(R.id.calibrationCard)
        seekBarWidth = findViewById(R.id.seekBarWidth)
        seekBarHeight = findViewById(R.id.seekBarHeight)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnSkip = findViewById(R.id.btnSkip)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvCalibrationInfo = findViewById(R.id.tvCalibrationInfo)
        
        // Setup seekbars
        seekBarWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateCardSize()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        seekBarHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateCardSize()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        btnConfirm.setOnClickListener { performCalibration() }
        btnSkip.setOnClickListener { skipCalibration() }
        
        // Set initial values
        seekBarWidth.progress = 300
        seekBarHeight.progress = 190
        updateCardSize()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.calibrationResult.collect { result ->
                    result?.let { handleCalibrationResult(it) }
                }
            }
        }
    }

    private fun updateCardSize() {
        val width = seekBarWidth.progress
        val height = seekBarHeight.progress
        
        calibrationCard.layoutParams = calibrationCard.layoutParams.apply {
            this.width = width
            this.height = height
        }
        
        calibrationCard.requestLayout()
    }

    private fun performCalibration() {
        val cardWidth = seekBarWidth.progress
        val cardHeight = seekBarHeight.progress
        
        viewModel.calibrate(cardWidth, cardHeight)
    }

    private fun skipCalibration() {
        navigateToMain()
    }

    private fun handleCalibrationResult(result: CalibrationResult) {
        when (result) {
            is CalibrationResult.Success -> {
                tvCalibrationInfo.text = result.summary
                tvCalibrationInfo.visibility = View.VISIBLE
                navigateToMain()
            }
            is CalibrationResult.Error -> {
                tvCalibrationInfo.text = "校准失败: ${result.message}"
                tvCalibrationInfo.visibility = View.VISIBLE
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun setupFocusHandling() {
        seekBarWidth.nextFocusDownId = R.id.seekBarHeight
        seekBarHeight.nextFocusDownId = R.id.btnConfirm
        btnConfirm.nextFocusUpId = R.id.seekBarHeight
        btnConfirm.nextFocusRightId = R.id.btnSkip
        btnSkip.nextFocusLeftId = R.id.btnConfirm
    }
}

/**
 * Calibration result
 */
sealed class CalibrationResult {
    data class Success(val summary: String) : CalibrationResult()
    data class Error(val message: String) : CalibrationResult()
}
