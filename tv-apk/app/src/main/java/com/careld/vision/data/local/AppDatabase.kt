package com.careld.vision.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.careld.vision.data.local.dao.ChildProfileDao
import com.careld.vision.data.local.dao.LocalConfigDao
import com.careld.vision.data.local.dao.SyncLogDao
import com.careld.vision.data.local.dao.VisionRecordDao
import com.careld.vision.data.local.entity.ChildProfileEntity
import com.careld.vision.data.local.entity.LocalConfigEntity
import com.careld.vision.data.local.entity.SyncLogEntity
import com.careld.vision.data.local.entity.VisionRecordEntity

/**
 * Room Database for Careld Vision TV
 * 
 * Contains tables for:
 * - Child profiles (local cache)
 * - Vision records (offline storage)
 * - Sync logs
 * - Local configuration
 */
@Database(
    entities = [
        ChildProfileEntity::class,
        VisionRecordEntity::class,
        SyncLogEntity::class,
        LocalConfigEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun childProfileDao(): ChildProfileDao
    abstract fun visionRecordDao(): VisionRecordDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun localConfigDao(): LocalConfigDao

    companion object {
        const val DATABASE_NAME = "careld_vision.db"
    }
}
