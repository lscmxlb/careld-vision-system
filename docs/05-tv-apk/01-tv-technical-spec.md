# 第五阶段：TV端技术方案

## 5.1 TV端概述

### 5.1.1 核心定位

安卓TV端是整个系统的核心交互终端，承载**电子视力表**这一关键业务功能。由于门店网络环境复杂（宽带/4G/断网），TV端必须具备**离线工作能力**。

### 5.1.2 技术挑战

| 挑战 | 说明 | 应对策略 |
|------|------|---------|
| 屏幕适配 | 各品牌电视分辨率、尺寸差异大 | 屏幕校准模块 |
| 视力精度 | 视标必须按物理尺寸显示 | 像素密度计算 + 校准 |
| 离线工作 | 断网时仍能检测和存储 | SQLite本地数据库 |
| 遥控器操作 | 无触屏，纯遥控器交互 | TV专用UI组件 |
| 网络切换 | 宽带/4G自动切换 | 网络状态监听 |

---

## 5.2 技术架构

### 5.2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      Careld TV APK                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    应用层 (App)                          │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │   │
│  │  │ Main     │ │ Vision   │ │ Child    │ │ Sync     │   │   │
│  │  │ Activity │ │ Activity │ │ Activity │ │ Activity │   │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    业务层 (Domain)                       │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │   │
│  │  │ Vision   │ │ Child    │ │ Sync     │ │ Config   │   │   │
│  │  │ Service  │ │ Service  │ │ Service  │ │ Service  │   │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    数据层 (Data)                         │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │   │
│  │  │ SQLite   │ │ Remote   │ │ Local    │ │ Sync     │   │   │
│  │  │ Database │ │ API      │ │ Cache    │ │ Queue    │   │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    基础层 (Core)                         │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │   │
│  │  │ Network  │ │ Security │ │ Storage  │ │ Utils    │   │   │
│  │  │ Manager  │ │ Manager  │ │ Manager  │ │          │   │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2.2 模块划分

```
app/src/main/java/com/careld/vision/
├── App.kt                      # Application入口
├── MainActivity.kt             # 主Activity
├── di/                         # 依赖注入
│   └── AppModule.kt
├── core/                       # 核心层
│   ├── network/                # 网络管理
│   │   ├── NetworkManager.kt
│   │   └── ApiService.kt
│   ├── security/               # 安全加密
│   │   └── EncryptionManager.kt
│   └── utils/                  # 工具类
│       ├── ScreenUtils.kt
│       └── DateUtils.kt
├── data/                       # 数据层
│   ├── local/                  # 本地数据
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   └── entity/
│   ├── remote/                 # 远程数据
│   │   ├── SyncApi.kt
│   │   └── AuthApi.kt
│   └── repository/             # 数据仓库
│       ├── ChildRepository.kt
│       ├── VisionRepository.kt
│       └── SyncRepository.kt
├── domain/                     # 业务层
│   ├── model/                  # 领域模型
│   │   ├── Child.kt
│   │   ├── VisionRecord.kt
│   │   └── SyncResult.kt
│   └── usecase/                # 用例
│       ├── GetChildrenUseCase.kt
│       ├── SaveVisionRecordUseCase.kt
│       └── SyncDataUseCase.kt
├── ui/                         # UI层
│   ├── base/                   # 基础组件
│   │   ├── BaseActivity.kt
│   │   └── TvFocusHelper.kt
│   ├── vision/                 # 视力表模块
│   │   ├── VisionActivity.kt
│   │   ├── VisionViewModel.kt
│   │   └── VisionChartView.kt
│   ├── child/                  # 儿童档案模块
│   │   ├── ChildSearchActivity.kt
│   │   └── ChildAdapter.kt
│   ├── sync/                   # 同步模块
│   │   ├── SyncActivity.kt
│   │   └── SyncViewModel.kt
│   └── settings/               # 设置模块
│       └── SettingsActivity.kt
└── service/                    # 后台服务
    ├── SyncService.kt
    └── NetworkMonitorService.kt
```

---

## 5.3 电子视力表组件

### 5.3.1 视力表标准

依据 **GB 11533-2011《标准对数视力表》** 实现

```
┌─────────────────────────────────────────────────────────────────┐
│                     标准对数视力表                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  5.3  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■  │
│  5.2  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■   │
│  5.1  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■    │
│  5.0  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■     │
│  4.9  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■      │
│  4.8  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■       │
│  4.7  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■        │
│  4.6  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■         │
│  4.5  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■          │
│  4.4  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■           │
│  4.3  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■            │
│  4.2  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■             │
│  4.1  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■              │
│  4.0  ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■               │
│                                                                 │
│  检测距离: 5米    当前检测: 左眼    模式: 标准对数视力表         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3.2 视标尺寸计算

```kotlin
/**
 * 视力表视标尺寸计算器
 * 依据GB 11533-2011标准
 */
class VisionChartCalculator {
    
    companion object {
        // 标准检测距离（米）
        const val STANDARD_DISTANCE = 5.0
        
        // 1分视角对应的弧度
        const val MINUTE_ANGLE_RADIAN = Math.PI / (180 * 60)
        
        // 视力等级对应视角（分）
        val VISION_ANGLES = mapOf(
            "4.0" to 10.0,    // 10分视角
            "4.1" to 7.943,
            "4.2" to 6.310,
            "4.3" to 5.012,
            "4.4" to 3.981,
            "4.5" to 3.162,
            "4.6" to 2.512,
            "4.7" to 1.995,
            "4.8" to 1.585,
            "4.9" to 1.259,
            "5.0" to 1.0,     // 1分视角（标准视力）
            "5.1" to 0.794,
            "5.2" to 0.631,
            "5.3" to 0.501
        )
    }
    
    /**
     * 计算视标边长（毫米）
     * @param visionLevel 视力等级（如"5.0"）
     * @param testDistance 检测距离（米）
     * @return 视标边长（毫米）
     */
    fun calculateOptotypeSize(
        visionLevel: String,
        testDistance: Double = STANDARD_DISTANCE
    ): Double {
        val angleInMinutes = VISION_ANGLES[visionLevel] 
            ?: throw IllegalArgumentException("Invalid vision level")
        
        // 视标边长 = 2 * 检测距离 * tan(视角/2)
        // 视角转换为弧度
        val angleInRadians = Math.toRadians(angleInMinutes / 60)
        
        // 计算边长（毫米）
        val sizeInMeters = 2 * testDistance * Math.tan(angleInRadians / 2)
        return sizeInMeters * 1000 // 转换为毫米
    }
    
    /**
     * 计算视标像素尺寸
     * @param sizeInMm 视标边长（毫米）
     * @param pixelPerMm 每毫米像素数（由屏幕校准得出）
     * @return 视标像素尺寸
     */
    fun mmToPixel(sizeInMm: Double, pixelPerMm: Double): Int {
        return (sizeInMm * pixelPerMm).toInt()
    }
}
```

### 5.3.3 屏幕校准模块

```kotlin
/**
 * 屏幕校准管理器
 * 首次使用或更换电视时需要校准
 */
class CalibrationManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("calibration", Context.MODE_PRIVATE)
    
    data class CalibrationData(
        val pixelPerMm: Double,        // 每毫米像素数
        val screenWidthMm: Double,     // 屏幕宽度（毫米）
        val screenHeightMm: Double,    // 屏幕高度（毫米）
        val screenSizeInch: Double,    // 屏幕尺寸（英寸）
        val calibrationTime: Long,     // 校准时间
        val isCalibrated: Boolean      // 是否已校准
    )
    
    /**
     * 执行屏幕校准
     * 用户放置一张标准信用卡（85.60mm × 53.98mm）在屏幕上
     * 通过遥控器调整屏幕上的虚拟卡片大小，使其与实际卡片重合
     */
    suspend fun calibrate(
        cardWidthPixel: Int,
        cardHeightPixel: Int
    ): CalibrationData {
        // 标准信用卡尺寸
        val cardWidthMm = 85.60
        val cardHeightMm = 53.98
        
        // 计算像素密度
        val pixelPerMmWidth = cardWidthPixel / cardWidthMm
        val pixelPerMmHeight = cardHeightPixel / cardHeightMm
        val pixelPerMm = (pixelPerMmWidth + pixelPerMmHeight) / 2
        
        // 获取屏幕像素尺寸
        val displayMetrics = context.resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val screenHeightPx = displayMetrics.heightPixels
        
        // 计算屏幕物理尺寸
        val screenWidthMm = screenWidthPx / pixelPerMm
        val screenHeightMm = screenHeightPx / pixelPerMm
        val screenSizeInch = Math.sqrt(
            screenWidthMm * screenWidthMm + screenHeightMm * screenHeightMm
        ) / 25.4
        
        val data = CalibrationData(
            pixelPerMm = pixelPerMm,
            screenWidthMm = screenWidthMm,
            screenHeightMm = screenHeightMm,
            screenSizeInch = screenSizeInch,
            calibrationTime = System.currentTimeMillis(),
            isCalibrated = true
        )
        
        saveCalibrationData(data)
        return data
    }
    
    fun getCalibrationData(): CalibrationData? {
        if (!prefs.getBoolean("is_calibrated", false)) return null
        
        return CalibrationData(
            pixelPerMm = prefs.getFloat("pixel_per_mm", 0f).toDouble(),
            screenWidthMm = prefs.getFloat("screen_width_mm", 0f).toDouble(),
            screenHeightMm = prefs.getFloat("screen_height_mm", 0f).toDouble(),
            screenSizeInch = prefs.getFloat("screen_size_inch", 0f).toDouble(),
            calibrationTime = prefs.getLong("calibration_time", 0),
            isCalibrated = true
        )
    }
    
    private fun saveCalibrationData(data: CalibrationData) {
        prefs.edit().apply {
            putFloat("pixel_per_mm", data.pixelPerMm.toFloat())
            putFloat("screen_width_mm", data.screenWidthMm.toFloat())
            putFloat("screen_height_mm", data.screenHeightMm.toFloat())
            putFloat("screen_size_inch", data.screenSizeInch.toFloat())
            putLong("calibration_time", data.calibrationTime)
            putBoolean("is_calibrated", true)
            apply()
        }
    }
}
```

### 5.3.4 视力表自定义View

```kotlin
/**
 * 电子视力表自定义View
 * 支持遥控器和触控操作
 */
class VisionChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var calibrationData: CalibrationManager.CalibrationData? = null
    private var currentVisionLevel = "5.0"
    private var testDistance = 5.0
    
    // 当前行视标列表
    private var optotypes: List<Optotype> = emptyList()
    private var currentIndex = 0
    
    // 回调
    var onVisionResult: ((String) -> Unit)? = null
    var onDirectionKey: ((Direction) -> Boolean)? = null
    
    enum class Direction { UP, DOWN, LEFT, RIGHT }
    
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
    }
    
    fun setCalibrationData(data: CalibrationManager.CalibrationData) {
        this.calibrationData = data
        invalidate()
    }
    
    fun setTestConfig(visionLevel: String, distance: Double) {
        this.currentVisionLevel = visionLevel
        this.testDistance = distance
        generateOptotypes()
        invalidate()
    }
    
    /**
     * 生成当前行的视标
     */
    private fun generateOptotypes() {
        val calculator = VisionChartCalculator()
        val sizeInMm = calculator.calculateOptotypeSize(currentVisionLevel, testDistance)
        val pixelPerMm = calibrationData?.pixelPerMm ?: 3.78
        val sizeInPixel = calculator.mmToPixel(sizeInMm, pixelPerMm)
        
        // 生成5个随机方向的视标
        val directions = listOf(
            Direction.UP, Direction.DOWN, 
            Direction.LEFT, Direction.RIGHT, 
            Direction.entries.random()
        )
        
        val screenWidth = width
        val spacing = sizeInPixel * 2 // 视标间距
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
        
        // 绘制背景
        canvas.drawColor(Color.BLACK)
        
        // 绘制视标
        optotypes.forEachIndexed { index, optotype ->
            paint.color = if (index == currentIndex) Color.CYAN else Color.WHITE
            drawOptotype(canvas, optotype)
        }
        
        // 绘制视力等级标识
        paint.color = Color.WHITE
        paint.textSize = 48f
        canvas.drawText(
            "视力: ${currentVisionLevel}",
            50f,
            100f,
            paint
        )
    }
    
    /**
     * 绘制单个视标（"E"字形）
     */
    private fun drawOptotype(canvas: Canvas, optotype: Optotype) {
        canvas.save()
        
        // 旋转画布
        val rotation = when (optotype.direction) {
            Direction.UP -> 0f
            Direction.RIGHT -> 90f
            Direction.DOWN -> 180f
            Direction.LEFT -> 270f
        }
        
        canvas.rotate(rotation, optotype.rect.centerX(), optotype.rect.centerY())
        
        // 绘制"E"字形（三横一竖）
        val strokeWidth = optotype.rect.width() / 5
        paint.strokeWidth = strokeWidth.toFloat()
        paint.style = Paint.Style.STROKE
        
        // 外框
        canvas.drawRect(optotype.rect, paint)
        
        // 中间横线
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
     * 处理遥控器按键
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                return checkDirection(VisionChartView.Direction.UP)
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                return checkDirection(VisionChartView.Direction.DOWN)
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                return checkDirection(VisionChartView.Direction.LEFT)
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                return checkDirection(VisionChartView.Direction.RIGHT)
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                // 确认当前选择
                onVisionResult?.invoke(currentVisionLevel)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    private fun checkDirection(direction: Direction): Boolean {
        val currentOptotype = optotypes.getOrNull(currentIndex)
        return if (currentOptotype?.direction == direction) {
            // 回答正确，移动到下一个
            currentIndex++
            if (currentIndex >= optotypes.size) {
                // 本行全部正确，降低视力等级
                decreaseVisionLevel()
            }
            invalidate()
            true
        } else {
            // 回答错误，记录当前视力
            onVisionResult?.invoke(currentVisionLevel)
            false
        }
    }
    
    private fun decreaseVisionLevel() {
        val levels = listOf("5.3", "5.2", "5.1", "5.0", "4.9", "4.8", "4.7", "4.6", 
                          "4.5", "4.4", "4.3", "4.2", "4.1", "4.0")
        val currentIndex = levels.indexOf(currentVisionLevel)
        if (currentIndex < levels.size - 1) {
            currentVisionLevel = levels[currentIndex + 1]
            generateOptotypes()
        } else {
            // 已到达最低视力
            onVisionResult?.invoke("4.0")
        }
    }
}
```

---

## 5.4 离线同步机制

### 5.4.1 同步管理器

```kotlin
/**
 * 数据同步管理器
 * 负责TV端与云端的数据同步
 */
class SyncManager(
    private val context: Context,
    private val apiService: ApiService,
    private val database: AppDatabase
) {
    
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * 触发同步（上传本地数据 + 下载云端数据）
     */
    suspend fun sync(): SyncResult {
        return try {
            // 1. 上传本地未同步的视力记录
            val uploadResult = uploadVisionRecords()
            
            // 2. 下载云端更新的儿童档案
            val downloadResult = downloadChildProfiles()
            
            SyncResult.Success(
                uploadedCount = uploadResult.successCount,
                downloadedCount = downloadResult.count
            )
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "同步失败")
        }
    }
    
    /**
     * 上传视力检测记录
     */
    private suspend fun uploadVisionRecords(): UploadResult {
        val records = database.visionRecordDao().getUnsyncedRecords()
        if (records.isEmpty()) return UploadResult(0, 0, 0)
        
        val batchId = generateBatchId()
        val request = records.map { it.toUploadRequest() }
        
        return try {
            val response = apiService.uploadVisionRecords(
                SyncUploadRequest(
                    batchId = batchId,
                    deviceId = getDeviceId(),
                    storeId = getStoreId(),
                    records = request
                )
            )
            
            // 更新本地记录状态
            response.data.results.forEach { result ->
                database.visionRecordDao().updateSyncStatus(
                    localId = result.localId,
                    status = if (result.status == "success") 2 else 3,
                    cloudRecordId = result.cloudRecordId,
                    errorMsg = if (result.status != "success") result.message else null
                )
            }
            
            UploadResult(
                totalCount = response.data.totalCount,
                successCount = response.data.successCount,
                failCount = response.data.failCount
            )
        } catch (e: Exception) {
            // 标记为失败，增加重试次数
            records.forEach { record ->
                database.visionRecordDao().incrementRetryCount(record.localId)
            }
            throw e
        }
    }
    
    /**
     * 下载儿童档案
     */
    private suspend fun downloadChildProfiles(): DownloadResult {
        val lastSyncTime = getLastSyncTime()
        
        val response = apiService.downloadChildProfiles(
            storeId = getStoreId(),
            lastSyncTime = lastSyncTime,
            page = 1,
            size = 100
        )
        
        // 保存到本地数据库
        response.data.records.forEach { record ->
            database.childProfileDao().insertOrUpdate(record.toLocalEntity())
        }
        
        // 更新同步时间
        setLastSyncTime(response.data.syncTime)
        
        return DownloadResult(response.data.records.size, response.data.hasMore)
    }
    
    /**
     * 设置自动同步任务
     */
    fun scheduleAutoSync(intervalMinutes: Int = 30) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalMinutes.toLong(),
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "auto_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }
    
    data class SyncResult {
        data class Success(
            val uploadedCount: Int,
            val downloadedCount: Int
        ) : SyncResult()
        
        data class Error(val message: String) : SyncResult()
    }
    
    data class UploadResult(
        val totalCount: Int,
        val successCount: Int,
        val failCount: Int
    )
    
    data class DownloadResult(
        val count: Int,
        val hasMore: Boolean
    )
}
```

### 5.4.2 网络状态监听

```kotlin
/**
 * 网络状态监听服务
 * 监听网络变化，自动触发同步
 */
class NetworkMonitorService : Service() {
    
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    
    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) 
            as ConnectivityManager
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "网络已连接，触发同步")
                triggerSync()
            }
            
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG, "网络已断开")
            }
        }
        
        // 注册网络监听
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }
    
    private fun triggerSync() {
        CoroutineScope(Dispatchers.IO).launch {
            val syncManager = SyncManager.getInstance(applicationContext)
            syncManager.sync()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        private const val TAG = "NetworkMonitor"
    }
}
```

---

## 5.5 UI/UX设计

### 5.5.1 页面流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        TV端页面流程                              │
└─────────────────────────────────────────────────────────────────┘

    ┌─────────────┐
    │   启动页     │
    │  (Logo展示)  │
    └──────┬──────┘
           │
           ▼
    ┌─────────────┐     未绑定     ┌─────────────┐
    │  设备登录页   │───────────────▶│  门店绑定页  │
    │  (设备码校验) │                │ (输入门店编码)│
    └──────┬──────┘                └──────┬──────┘
           │                              │
           │ 已绑定                         │
           ▼                              ▼
    ┌─────────────┐                ┌─────────────┐
    │  校准检查    │◄───────────────│  绑定成功    │
    │ (是否已校准?) │                │             │
    └──────┬──────┘                └─────────────┘
           │
     未校准 │ 已校准
           ▼
    ┌─────────────┐
    │  屏幕校准页  │
    │ (信用卡校准) │
    └──────┬──────┘
           │
           ▼
    ┌─────────────┐
    │   主页      │
    │ (功能入口)  │
    └──────┬──────┘
           │
     ┌─────┼─────┐
     │     │     │
     ▼     ▼     ▼
┌────────┐┌────────┐┌────────┐
│视力检测││儿童检索││数据同步│
│        ││        ││        │
└───┬────┘└───┬────┘└───┬────┘
    │         │         │
    ▼         │         ▼
┌────────┐    │    ┌────────┐
│视力表页│    │    │同步状态│
│        │    │    │        │
└───┬────┘    │    └────────┘
    │         │
    ▼         │
┌────────┐    │
│结果录入│◄───┘
│        │
└────────┘
```

### 5.5.2 焦点导航设计

```kotlin
/**
 * TV焦点管理器
 * 处理遥控器焦点移动
 */
class TvFocusManager {
    
    /**
     * 设置焦点链
     * 确保焦点在可聚焦元素间循环移动
     */
    fun setupFocusChain(views: List<View>) {
        views.forEachIndexed { index, view ->
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            
            // 设置下一个焦点
            view.nextFocusForwardId = views.getOrNull(index + 1)?.id ?: views.first().id
            view.nextFocusDownId = views.getOrNull(index + 1)?.id ?: views.first().id
            view.nextFocusUpId = views.getOrNull(index - 1)?.id ?: views.last().id
        }
    }
    
    /**
     * 焦点变化动画
     */
    fun applyFocusAnimation(view: View, hasFocus: Boolean) {
        if (hasFocus) {
            view.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(150)
                .start()
            view.setBackgroundResource(R.drawable.bg_focused)
        } else {
            view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(150)
                .start()
            view.setBackgroundResource(R.drawable.bg_normal)
        }
    }
}
```

---

## 5.6 阶段交付物

1. **TV端技术方案文档** - 本文档
2. **电子视力表组件源码** - VisionChartView.kt
3. **屏幕校准模块** - CalibrationManager.kt
4. **离线同步模块** - SyncManager.kt
5. **TV端UI组件库** - TV专用组件
6. **APK构建配置** - build.gradle

---

## 5.7 测试要点

| 测试项 | 测试内容 |
|--------|---------|
| 屏幕校准 | 多品牌电视校准精度验证 |
| 视力精度 | 视标物理尺寸测量验证 |
| 离线功能 | 断网检测、存储、恢复同步 |
| 遥控器操作 | 各品牌遥控器兼容性 |
| 网络切换 | 宽带/4G切换稳定性 |
| 性能测试 | 启动时间、内存占用 |
| 兼容性 | Android 7.0-12.0 |
