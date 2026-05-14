package com.reps.app.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_logs",
    indices = [Index(value = ["date", "mealType"])]
)
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val mealType: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
