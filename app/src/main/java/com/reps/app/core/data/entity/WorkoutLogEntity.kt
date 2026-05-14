package com.reps.app.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_logs",
    indices = [Index("date")]
)
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val templateId: Long? = null,
    val name: String,
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
