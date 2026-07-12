package com.careld.vision.core.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.sqrt

/**
 * Screen utility functions
 * 
 * Provides screen dimension calculations and conversions.
 */
object ScreenUtils {

    /**
     * Get display metrics
     */
    fun getDisplayMetrics(context: Context): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return DisplayMetrics().apply {
            windowManager.defaultDisplay.getRealMetrics(this)
        }
    }

    /**
     * Get screen width in pixels
     */
    fun getScreenWidthPx(context: Context): Int {
        return getDisplayMetrics(context).widthPixels
    }

    /**
     * Get screen height in pixels
     */
    fun getScreenHeightPx(context: Context): Int {
        return getDisplayMetrics(context).heightPixels
    }

    /**
     * Get screen density (dpi)
     */
    fun getScreenDensity(context: Context): Float {
        return getDisplayMetrics(context).densityDpi.toFloat()
    }

    /**
     * Convert dp to pixels
     */
    fun dpToPx(context: Context, dp: Float): Float {
        return dp * getDisplayMetrics(context).density
    }

    /**
     * Convert pixels to dp
     */
    fun pxToDp(context: Context, px: Float): Float {
        return px / getDisplayMetrics(context).density
    }

    /**
     * Calculate screen diagonal size in inches
     */
    fun calculateScreenSizeInch(context: Context): Double {
        val metrics = getDisplayMetrics(context)
        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        return sqrt(widthInches * widthInches + heightInches * heightInches)
    }

    /**
     * Calculate pixels per millimeter
     * @param screenSizeInch Screen diagonal size in inches
     * @param screenWidthPx Screen width in pixels
     * @param screenHeightPx Screen height in pixels
     */
    fun calculatePixelPerMm(
        screenSizeInch: Double,
        screenWidthPx: Int,
        screenHeightPx: Int
    ): Double {
        // Calculate screen diagonal in pixels
        val diagonalPx = sqrt(
            (screenWidthPx * screenWidthPx + screenHeightPx * screenHeightPx).toDouble()
        )
        
        // Convert diagonal inches to mm (1 inch = 25.4 mm)
        val diagonalMm = screenSizeInch * 25.4
        
        // Pixels per mm
        return diagonalPx / diagonalMm
    }

    /**
     * Calculate physical width in mm
     */
    fun calculateScreenWidthMm(
        pixelPerMm: Double,
        screenWidthPx: Int
    ): Double {
        return screenWidthPx / pixelPerMm
    }

    /**
     * Calculate physical height in mm
     */
    fun calculateScreenHeightMm(
        pixelPerMm: Double,
        screenHeightPx: Int
    ): Double {
        return screenHeightPx / pixelPerMm
    }

    /**
     * Convert millimeters to pixels
     */
    fun mmToPx(mm: Double, pixelPerMm: Double): Int {
        return (mm * pixelPerMm).toInt()
    }

    /**
     * Convert pixels to millimeters
     */
    fun pxToMm(px: Int, pixelPerMm: Double): Double {
        return px / pixelPerMm
    }
}
