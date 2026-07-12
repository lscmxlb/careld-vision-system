package com.careld.vision.core.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Date and time utility functions
 */
object DateUtils {

    private const val DEFAULT_DATE_FORMAT = "yyyy-MM-dd"
    private const val DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
    private const val DISPLAY_DATE_FORMAT = "yyyy年MM月dd日"
    private const val DISPLAY_DATETIME_FORMAT = "MM月dd日 HH:mm"

    /**
     * Format timestamp to date string
     */
    fun formatDate(timestamp: Long, pattern: String = DEFAULT_DATE_FORMAT): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Format timestamp to datetime string
     */
    fun formatDateTime(timestamp: Long, pattern: String = DEFAULT_DATETIME_FORMAT): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Format for display
     */
    fun formatForDisplay(timestamp: Long): String {
        return formatDateTime(timestamp, DISPLAY_DATETIME_FORMAT)
    }

    /**
     * Parse date string to timestamp
     */
    fun parseDate(dateString: String, pattern: String = DEFAULT_DATE_FORMAT): Long? {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get current timestamp
     */
    fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Calculate age from birth date
     */
    fun calculateAge(birthDate: String): Int {
        val birthTime = parseDate(birthDate) ?: return 0
        val birthCalendar = Calendar.getInstance().apply { timeInMillis = birthTime }
        val nowCalendar = Calendar.getInstance()

        var age = nowCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        
        if (nowCalendar.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        
        return age
    }

    /**
     * Format duration
     */
    fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return when {
            hours > 0 -> String.format("%d小时%d分", hours, minutes)
            minutes > 0 -> String.format("%d分%d秒", minutes, seconds)
            else -> String.format("%d秒", seconds)
        }
    }

    /**
     * Get relative time description
     */
    fun getRelativeTime(timestamp: Long): String {
        val diff = currentTimeMillis() - timestamp
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "刚刚"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}分钟前"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}小时前"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}天前"
            else -> formatDate(timestamp)
        }
    }

    /**
     * Check if two timestamps are on the same day
     */
    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Get start of day timestamp
     */
    fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get end of day timestamp
     */
    fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}
