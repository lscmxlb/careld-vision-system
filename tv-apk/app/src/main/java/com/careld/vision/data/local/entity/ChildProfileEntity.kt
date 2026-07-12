package com.careld.vision.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local child profile entity
 * 
 * Stores cached child profile data from cloud for offline access.
 */
@Entity(
    tableName = "local_child_profile",
    indices = [
        Index(value = ["child_id"], unique = true),
        Index(value = ["sync_time"]),
        Index(value = ["name"])
    ]
)
data class ChildProfileEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "child_id")
    val childId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "phone")
    val phone: String?,

    @ColumnInfo(name = "birth_date")
    val birthDate: String?,

    @ColumnInfo(name = "gender")
    val gender: Int?, // 0: female, 1: male

    @ColumnInfo(name = "medical_history")
    val medicalHistory: String?,

    @ColumnInfo(name = "sync_time")
    val syncTime: Long,

    @ColumnInfo(name = "cloud_updated_at")
    val cloudUpdatedAt: Long?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
