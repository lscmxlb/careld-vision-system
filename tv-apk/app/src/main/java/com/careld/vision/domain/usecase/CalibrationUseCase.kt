package com.careld.vision.domain.usecase

import android.content.Context
import com.careld.vision.data.local.AppDatabase
import com.careld.vision.data.local.entity.LocalConfigEntity
import com.careld.vision.domain.model.CalibrationData
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Screen calibration use case
 * 
 * Handles screen calibration for accurate vision testing.
 */
@Singleton
class CalibrationUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    companion object {
        private const val TAG = "CalibrationUseCase"
        private const val PREFS_NAME = "calibration_prefs"
        private const val KEY_CALIBRATION_DATA = "calibration_data"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Check if screen is calibrated
     */
    suspend fun isCalibrated(): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean("is_calibrated", false)
    }

    /**
     * Get calibration data
     */
    suspend fun getCalibrationData(): CalibrationData? = withContext(Dispatchers.IO) {
        if (!isCalibrated()) return null
        
        val json = prefs.getString(KEY_CALIBRATION_DATA, null)
        json?.let { gson.fromJson(it, CalibrationData::class.java) }
    }

    /**
     * Perform screen calibration
     * 
     * Uses credit card calibration method:
     * 1. User places a standard credit card on screen
     * 2. User adjusts virtual card size to match physical card
     * 3. System calculates pixel density
     * 
     * @param cardWidthPixel Width of virtual card in pixels
     * @param cardHeightPixel Height of virtual card in pixels
     * @param screenWidthPx Screen width in pixels
     * @param screenHeightPx Screen height in pixels
     */
    suspend fun calibrate(
        cardWidthPixel: Int,
        cardHeightPixel: Int,
        screenWidthPx: Int,
        screenHeightPx: Int
    ): Result<CalibrationData> = withContext(Dispatchers.IO) {
        try {
            // Standard credit card dimensions
            val cardWidthMm = CalibrationData.STANDARD_CREDIT_CARD_WIDTH_MM
            val cardHeightMm = CalibrationData.STANDARD_CREDIT_CARD_HEIGHT_MM
            
            // Calculate pixel per mm from both dimensions and average
            val pixelPerMmWidth = cardWidthPixel / cardWidthMm
            val pixelPerMmHeight = cardHeightPixel / cardHeightMm
            val pixelPerMm = (pixelPerMmWidth + pixelPerMmHeight) / 2.0
            
            // Calculate screen physical dimensions
            val screenWidthMm = screenWidthPx / pixelPerMm
            val screenHeightMm = screenHeightPx / pixelPerMm
            
            // Calculate diagonal size in inches
            val diagonalMm = kotlin.math.sqrt(
                screenWidthMm * screenWidthMm + screenHeightMm * screenHeightMm
            )
            val screenSizeInch = diagonalMm / 25.4
            
            val calibrationData = CalibrationData(
                pixelPerMm = pixelPerMm,
                screenWidthMm = screenWidthMm,
                screenHeightMm = screenHeightMm,
                screenSizeInch = screenSizeInch,
                calibrationTime = System.currentTimeMillis(),
                isCalibrated = true
            )
            
            // Save calibration data
            saveCalibrationData(calibrationData)
            
            Result.success(calibrationData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save calibration data
     */
    suspend fun saveCalibrationData(data: CalibrationData) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString(KEY_CALIBRATION_DATA, gson.toJson(data))
            putBoolean("is_calibrated", true)
            apply()
        }
        
        // Also save to database
        database.localConfigDao().updateValue(
            LocalConfigEntity.Keys.CALIBRATION_DATA,
            gson.toJson(data)
        )
    }

    /**
     * Clear calibration data
     */
    suspend fun clearCalibration() = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            remove(KEY_CALIBRATION_DATA)
            putBoolean("is_calibrated", false)
            apply()
        }
        
        database.localConfigDao().updateValue(
            LocalConfigEntity.Keys.CALIBRATION_DATA,
            null
        )
    }

    /**
     * Validate calibration accuracy
     * 
     * @param knownMm Known physical size in millimeters
     * @param measuredPx Measured size in pixels
     * @return Error percentage (should be < 5% for good calibration)
     */
    suspend fun validateCalibration(
        knownMm: Double,
        measuredPx: Int
    ): Double = withContext(Dispatchers.IO) {
        val calibrationData = getCalibrationData()
            ?: throw IllegalStateException("Not calibrated")
        
        val expectedPx = calibrationData.mmToPx(knownMm)
        val error = kotlin.math.abs(measuredPx - expectedPx).toDouble() / expectedPx * 100
        
        error
    }

    /**
     * Get calibration summary
     */
    suspend fun getCalibrationSummary(): String = withContext(Dispatchers.IO) {
        val data = getCalibrationData()
        data?.getSummary() ?: "未校准"
    }
}
