package com.reps.app.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_plan_templates")
data class MealPlanTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val cuisineType: String,
    val daysPerWeek: Int = 7,
    val mealsPerDay: Int = 3,
    val isCustom: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
