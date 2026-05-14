package com.reps.app.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_log_entries",
    foreignKeys = [
        ForeignKey(
            entity = MealLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealLogId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("mealLogId"), Index("foodItemId")]
)
data class MealLogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealLogId: Long,
    val foodItemId: Long,
    val servingMultiplier: Double = 1.0,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
