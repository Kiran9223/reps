package com.reps.app.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val muscleGroups: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val isShoulderSafe: Boolean = true,
    val restrictedMovements: List<String> = emptyList(),
    val instructions: String? = null,
    val source: String,
    val externalId: String? = null,
    val isCustom: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
