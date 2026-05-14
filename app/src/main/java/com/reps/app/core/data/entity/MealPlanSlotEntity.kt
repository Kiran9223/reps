package com.reps.app.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_plan_slots",
    foreignKeys = [
        ForeignKey(
            entity = MealPlanTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("templateId"), Index("foodItemId")]
)
data class MealPlanSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val dayOfWeek: Int,
    val mealType: String,
    val foodItemId: Long,
    val servingMultiplier: Double = 1.0,
    val sortOrder: Int = 0
)
