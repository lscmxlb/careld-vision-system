package com.careld.vision.ui.vision

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import com.careld.vision.domain.model.CalibrationData
import com.careld.vision.domain.usecase.VisionTestUseCase

/**
 * Electronic vision chart view
 * 
 * Custom view for displaying standard logarithmic vision chart.
 * Implements GB 11533-2011 standard.
 */
class VisionChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var calibrationData: CalibrationData? = null
    private var currentVisionLevel = "5.0"
    private var testDistance = 5.0
    
    // Current row optotypes
    private var optotypes: List<Optotype> = emptyList()
    private var currentIndex = 0
    
    // Callbacks
    var onVisionResult: ((String) -> Unit)? = null
    var onDirectionInput: ((Direction) -> Boolean)? = null
    
    // State
    private var isTesting = false
    
    enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }
    
    data class Optotype(
        val direction: Direction,
        val rect: RectF,
        val visionLevel: String
    )
    
    init {
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(Color.BLACK)
    }
    
    /**
     * Set calibration data
     */
    fun setCalibrationData(data: CalibrationData) {
        this.calibrationData = data
        invalidate()
    }
    
    /**
     * Set test configuration
     */
    fun setTestConfig(visionLevel: String, distance: Double = 5.0) {
        this.currentVisionLevel = visionLevel
        this.testDistance = distance
        generateOptotypes()
        invalidate()
    }
    
    /**
     * Start vision test
     */
    fun startTest() {
        isTesting = true
        currentIndex = 0
        generateOptotypes()
        invalidate()
    }
    
    /**
     * Stop vision test
     */
    fun stopTest() {
        isTesting = false
        invalidate()
    }
    
    /**
     * Generate optotypes for current vision level
     */
    private fun generateOptotypes() {
        val calData = calibrationData ?: return
        
        val sizeInPixel = calData.calculateOptotypePixelSize(currentVisionLevel, testDistance)
        
        // Generate 5 random directions
        val directions = List(5) { Direction.entries.random() }
        
        val screenWidth = width
        val spacing = sizeInPixel * 2
        val totalWidth = directions.size * sizeInPixel + (directions.size - 1) * spacing
        val startX = (screenWidth - totalWidth) / 2f
        val centerY = height / 2f
        
        optotypes = directions.mapIndexed { index, direction ->
            val x = startX + index * (sizeInPixel + spacing)
            val rect = RectF(
                x,
                centerY - sizeInPixel / 2f,
                x + sizeInPixel,
                centerY + sizeInPixel / 2f
            )
            Optotype(direction, rect, currentVisionLevel)
        }
        
        currentIndex = 0
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background
        canvas.drawColor(Color.BLACK)
        
        if (!isTesting || calibrationData == null) {
            drawWaitingScreen(canvas)
            return
        }
        
        // Draw optotypes
        optotypes.forEachIndexed { index, optotype ->
            paint.color = if (index == currentIndex) {
                Color.CYAN
            } else {
                Color.WHITE
            }
            drawOptotype(canvas, optotype)
        }
        
        // Draw info
        drawInfo(canvas)
    }
    
    /**
     * Draw waiting screen
     */
    private fun drawWaitingScreen(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.textSize = 48f
        paint.textAlign = Paint.Align.CENTER
        
        val message = if (calibrationData == null) {
            "请先进行屏幕校准"
        } else {
            "按确认键开始检测"
        }
        
        canvas.drawText(
            message,
            width / 2f,
            height / 2f,
            paint
        )
    }
    
    /**
     * Draw single optotype (E shape)
     */
    private fun drawOptotype(canvas: Canvas, optotype: Optotype) {
        canvas.save()
        
        // Rotate based on direction
        val rotation = when (optotype.direction) {
            Direction.UP -> 0f
            Direction.RIGHT -> 90f
            Direction.DOWN -> 180f
            Direction.LEFT -> 270f
        }
        
        canvas.rotate(
            rotation,
            optotype.rect.centerX(),
            optotype.rect.centerY()
        )
        
        // Draw E shape
        val strokeWidth = optotype.rect.width() / 5
        paint.strokeWidth = strokeWidth
        paint.style = Paint.Style.STROKE
        
        // Outer frame
        canvas.drawRect(optotype.rect, paint)
        
        // Middle horizontal line
        val midY = optotype.rect.centerY()
        canvas.drawLine(
            optotype.rect.left,
            midY,
            optotype.rect.right - strokeWidth,
            midY,
            paint
        )
        
        canvas.restore()
    }
    
    /**
     * Draw info text
     */
    private fun drawInfo(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.textSize = 36f
        paint.textAlign = Paint.Align.LEFT
        paint.style = Paint.Style.FILL
        
        // Vision level
        canvas.drawText("当前视力: $currentVisionLevel", 50f, 80f, paint)
        
        // Test distance
        canvas.drawText("检测距离: ${testDistance}米", 50f, 130f, paint)
        
        // Current eye
        val eyeText = if (currentIndex % 2 == 0) "左眼" else "右眼"
        canvas.drawText("检测眼: $eyeText", 50f, 180f, paint)
        
        // Instructions
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "请使用方向键指示缺口方向",
            width / 2f,
            height - 100f,
            paint
        )
    }
    
    /**
     * Handle key events
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isTesting) {
            if (keyCode == KeyEvent.KEYCODE_ENTER || 
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                startTest()
                return true
            }
            return super.onKeyDown(keyCode, event)
        }
        
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> checkDirection(Direction.UP)
            KeyEvent.KEYCODE_DPAD_DOWN -> checkDirection(Direction.DOWN)
            KeyEvent.KEYCODE_DPAD_LEFT -> checkDirection(Direction.LEFT)
            KeyEvent.KEYCODE_DPAD_RIGHT -> checkDirection(Direction.RIGHT)
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                // Confirm current selection
                onVisionResult?.invoke(currentVisionLevel)
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                stopTest()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    /**
     * Check if direction input matches current optotype
     */
    private fun checkDirection(direction: Direction): Boolean {
        val currentOptotype = optotypes.getOrNull(currentIndex)
        
        return if (currentOptotype?.direction == direction) {
            // Correct - move to next
            currentIndex++
            
            if (currentIndex >= optotypes.size) {
                // All correct in this row - go to next level (worse vision)
                decreaseVisionLevel()
            }
            
            invalidate()
            onDirectionInput?.invoke(direction) ?: true
        } else {
            // Incorrect - record result
            onVisionResult?.invoke(currentVisionLevel)
            onDirectionInput?.invoke(direction) ?: false
        }
    }
    
    /**
     * Decrease vision level (worse vision)
     */
    private fun decreaseVisionLevel() {
        val levels = listOf(
            "5.3", "5.2", "5.1", "5.0", "4.9", "4.8", "4.7", "4.6",
            "4.5", "4.4", "4.3", "4.2", "4.1", "4.0"
        )
        
        val currentIndex = levels.indexOf(currentVisionLevel)
        if (currentIndex < levels.size - 1) {
            currentVisionLevel = levels[currentIndex + 1]
            generateOptotypes()
        } else {
            // Minimum vision reached
            onVisionResult?.invoke("4.0")
        }
    }
    
    /**
     * Get current vision level
     */
    fun getCurrentVisionLevel(): String = currentVisionLevel
}
