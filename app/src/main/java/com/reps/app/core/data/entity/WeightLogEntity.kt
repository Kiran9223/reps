package com.reps.app.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_logs",
    indices = [Index(value = ["date"], unique = true)]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weightKg: Double,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
