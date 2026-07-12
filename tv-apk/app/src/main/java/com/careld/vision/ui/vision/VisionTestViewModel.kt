package com.careld.vision.ui.vision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.careld.vision.domain.model.CalibrationData
import com.careld.vision.domain.model.VisionRecord
import com.careld.vision.domain.usecase.CalibrationUseCase
import com.careld.vision.domain.usecase.VisionTestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Vision test view model
 */
@HiltViewModel
class VisionTestViewModel @Inject constructor(
    private val visionTestUseCase: VisionTestUseCase,
    private val calibrationUseCase: CalibrationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisionTestUiState())
    val uiState: StateFlow<VisionTestUiState> = _uiState.asStateFlow()
    
    private val _calibrationData = MutableStateFlow<CalibrationData?>(null)
    val calibrationData: StateFlow<CalibrationData?> = _calibrationData.asStateFlow()

    init {
        loadCalibrationData()
    }

    private fun loadCalibrationData() {
        viewModelScope.launch {
            _calibrationData.value = calibrationUseCase.getCalibrationData()
        }
    }

    fun selectEye(eye: Eye) {
        _uiState.value = _uiState.value.copy(
            selectedEye = eye,
            canStartTest = canStartTest()
        )
    }

    fun selectType(type: TestType) {
        _uiState.value = _uiState.value.copy(
            selectedType = type,
            canStartTest = canStartTest()
        )
    }

    fun onVisionResult(visionLevel: String) {
        _uiState.value = _uiState.value.copy(
            currentVisionLevel = visionLevel,
            testResult = visionLevel
        )
    }

    fun saveResult() {
        val state = _uiState.value
        val visionLevel = state.testResult ?: return
        
        viewModelScope.launch {
            visionTestUseCase.saveVisionRecord(
                childId = "CHILD_001", // Would come from selected child
                eyeType = when (state.selectedEye) {
                    Eye.LEFT -> VisionRecord.EyeType.LEFT
                    Eye.RIGHT -> VisionRecord.EyeType.RIGHT
                    null -> VisionRecord.EyeType.LEFT
                },
                visionLevel = visionLevel,
                testType = when (state.selectedType) {
                    TestType.BEFORE -> VisionRecord.TestType.BEFORE
                    TestType.AFTER -> VisionRecord.TestType.AFTER
                    null -> VisionRecord.TestType.BEFORE
                },
                testerName = null,
                remark = null
            )
        }
    }

    private fun canStartTest(): Boolean {
        return _uiState.value.selectedEye != null && 
               _uiState.value.selectedType != null
    }

    enum class Eye(val displayName: String) {
        LEFT("左眼"),
        RIGHT("右眼")
    }

    enum class TestType(val displayName: String) {
        BEFORE("养护前"),
        AFTER("养护后")
    }
}
