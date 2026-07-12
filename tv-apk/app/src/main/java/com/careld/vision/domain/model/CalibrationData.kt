package com.careld.vision.domain.model

/**
 * Screen calibration data
 */
data class CalibrationData(
    val pixelPerMm: Double,
    val screenWidthMm: Double,
    val screenHeightMm: Double,
    val screenSizeInch: Double,
    val calibrationTime: Long,
    val isCalibrated: Boolean
) {
    companion object {
        const val STANDARD_CREDIT_CARD_WIDTH_MM = 85.60
        const val STANDARD_CREDIT_CARD_HEIGHT_MM = 53.98
    }

    /**
     * Convert millimeters to pixels
     */
    fun mmToPx(mm: Double): Int {
        return (mm * pixelPerMm).toInt()
    }

    /**
     * Convert pixels to millimeters
     */
    fun pxToMm(px: Int): Double {
        return px / pixelPerMm
    }

    /**
     * Calculate optotype size in pixels for given vision level
     * Based on GB 11533-2011 standard
     */
    fun calculateOptotypePixelSize(
        visionLevel: String,
        testDistance: Double = 5.0
    ): Int {
        val angleInMinutes = VisionLevelAngles.ANGLES[visionLevel] 
            ?: throw IllegalArgumentException("Invalid vision level: $visionLevel")
        
        // Convert angle to radians
        val angleInRadians = Math.toRadians(angleInMinutes / 60.0)
        
        // Calculate size in meters: size = 2 * distance * tan(angle/2)
        val sizeInMeters = 2 * testDistance * kotlin.math.tan(angleInRadians / 2)
        val sizeInMm = sizeInMeters * 1000
        
        return mmToPx(sizeInMm)
    }

    /**
     * Get calibration summary
     */
    fun getSummary(): String {
        return buildString {
            appendLine("屏幕尺寸: ${String.format("%.1f", screenSizeInch)}英寸")
            appendLine("物理尺寸: ${String.format("%.1f", screenWidthMm)} × ${String.format("%.1f", screenHeightMm)} mm")
            appendLine("像素密度: ${String.format("%.2f", pixelPerMm)} px/mm")
            appendLine("校准时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(calibrationTime))}")
        }
    }

    /**
     * Vision level angles lookup (in arcminutes)
     * Based on GB 11533-2011 standard
     */
    object VisionLevelAngles {
        val ANGLES = mapOf(
            "4.0" to 10.0,
            "4.1" to 7.943,
            "4.2" to 6.310,
            "4.3" to 5.012,
            "4.4" to 3.981,
            "4.5" to 3.162,
            "4.6" to 2.512,
            "4.7" to 1.995,
            "4.8" to 1.585,
            "4.9" to 1.259,
            "5.0" to 1.0,
            "5.1" to 0.794,
            "5.2" to 0.631,
            "5.3" to 0.501
        )

        val LEVELS = listOf(
            "4.0", "4.1", "4.2", "4.3", "4.4", "4.5", "4.6", "4.7",
            "4.8", "4.9", "5.0", "5.1", "5.2", "5.3"
        )
    }
}
