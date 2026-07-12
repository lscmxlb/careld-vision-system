package com.careld.vision.domain.usecase

import com.careld.vision.data.local.AppDatabase
import com.careld.vision.data.local.entity.VisionRecordEntity
import com.careld.vision.domain.model.CalibrationData
import com.careld.vision.domain.model.VisionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vision test use case
 * 
 * Handles vision testing logic and record management.
 */
@Singleton
class VisionTestUseCase @Inject constructor(
    private val database: AppDatabase
) {

    companion object {
        private const val TAG = "VisionTestUseCase"
    }

    /**
     * Vision levels in descending order (best to worst)
     */
    val visionLevels = listOf(
        "5.3", "5.2", "5.1", "5.0",
        "4.9", "4.8", "4.7", "4.6",
        "4.5", "4.4", "4.3", "4.2",
        "4.1", "4.0"
    )

    /**
     * Get next vision level (worse vision)
     */
    fun getNextLevel(currentLevel: String): String? {
        val index = visionLevels.indexOf(currentLevel)
        return if (index < visionLevels.size - 1) {
            visionLevels[index + 1]
        } else {
            null
        }
    }

    /**
     * Get previous vision level (better vision)
     */
    fun getPreviousLevel(currentLevel: String): String? {
        val index = visionLevels.indexOf(currentLevel)
        return if (index > 0) {
            visionLevels[index - 1]
        } else {
            null
        }
    }

    /**
     * Calculate optotype pixel size for given vision level
     */
    fun calculateOptotypeSize(
        visionLevel: String,
        calibrationData: CalibrationData,
        testDistance: Double = 5.0
    ): Int {
        return calibrationData.calculateOptotypePixelSize(visionLevel, testDistance)
    }

    /**
     * Save vision test record
     */
    suspend fun saveVisionRecord(
        childId: String,
        eyeType: VisionRecord.EyeType,
        visionLevel: String,
        testType: VisionRecord.TestType,
        testerName: String?,
        remark: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val localId = generateLocalId()
            
            val entity = VisionRecordEntity(
                localId = localId,
                childId = childId,
                eyeType = VisionRecord.toEyeTypeString(eyeType),
                visionLevel = visionLevel,
                testTime = System.currentTimeMillis(),
                beforeAfter = VisionRecord.toTestTypeString(testType),
                testerName = testerName,
                remark = remark,
                syncStatus = VisionRecordEntity.UNSYNCED,
                retryCount = 0,
                lastSyncTime = null,
                errorMsg = null,
                cloudRecordId = null
            )
            
            database.visionRecordDao().insert(entity)
            
            Result.success(localId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get vision records for a child
     */
    fun getVisionRecordsForChild(childId: String) = 
        database.visionRecordDao().getByChildId(childId)

    /**
     * Get latest vision record for child and type
     */
    suspend fun getLatestVisionRecord(
        childId: String,
        testType: VisionRecord.TestType,
        eyeType: VisionRecord.EyeType
    ): VisionRecordEntity? = withContext(Dispatchers.IO) {
        database.visionRecordDao().getLatestByChildAndType(
            childId = childId,
            beforeAfter = VisionRecord.toTestTypeString(testType),
            eyeType = VisionRecord.toEyeTypeString(eyeType)
        )
    }

    /**
     * Compare before and after vision test results
     */
    fun compareVisionResults(
        beforeLevel: String,
        afterLevel: String
    ): VisionComparison {
        val beforeIndex = visionLevels.indexOf(beforeLevel)
        val afterIndex = visionLevels.indexOf(afterLevel)
        
        return when {
            afterIndex < beforeIndex -> VisionComparison.IMPROVED
            afterIndex > beforeIndex -> VisionComparison.WORSENED
            else -> VisionComparison.UNCHANGED
        }
    }

    /**
     * Get vision improvement value
     */
    fun getVisionImprovement(
        beforeLevel: String,
        afterLevel: String
    ): String {
        val beforeValue = beforeLevel.toDoubleOrNull() ?: 0.0
        val afterValue = afterLevel.toDoubleOrNull() ?: 0.0
        val diff = afterValue - beforeValue
        
        return if (diff >= 0) {
            "+${String.format("%.1f", diff)}"
        } else {
            String.format("%.1f", diff)
        }
    }

    /**
     * Generate random optotype directions
     */
    fun generateOptotypeDirections(count: Int = 5): List<VisionRecordDirection> {
        return List(count) { VisionRecordDirection.entries.random() }
    }

    private fun generateLocalId(): String {
        return "local_${UUID.randomUUID().toString().replace("-", "").take(20)}"
    }

    /**
     * Vision comparison result
     */
    enum class VisionComparison {
        IMPROVED, WORSENED, UNCHANGED
    }

    /**
     * Vision record direction
     */
    enum class VisionRecordDirection {
        UP, DOWN, LEFT, RIGHT
    }
}
