package com.careld.vision.ui.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.careld.vision.core.utils.ScreenUtils
import com.careld.vision.domain.usecase.CalibrationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Calibration view model
 */
@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationUseCase: CalibrationUseCase
) : ViewModel() {

    private val _calibrationResult = MutableSharedFlow<CalibrationResult?>()
    val calibrationResult: SharedFlow<CalibrationResult?> = _calibrationResult.asSharedFlow()

    fun calibrate(cardWidthPixel: Int, cardHeightPixel: Int) {
        viewModelScope.launch {
            try {
                // Get screen dimensions
                val screenWidthPx = 1920 // Would get from actual screen
                val screenHeightPx = 1080
                
                val result = calibrationUseCase.calibrate(
                    cardWidthPixel = cardWidthPixel,
                    cardHeightPixel = cardHeightPixel,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx
                )
                
                result.fold(
                    onSuccess = { data ->
                        _calibrationResult.emit(CalibrationResult.Success(data.getSummary()))
                    },
                    onFailure = { error ->
                        _calibrationResult.emit(CalibrationResult.Error(error.message ?: "Unknown error"))
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Calibration failed")
                _calibrationResult.emit(CalibrationResult.Error(e.message ?: "Calibration failed"))
            }
        }
    }
}
