package com.reps.app.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_items",
    indices = [Index(value = ["externalId", "source"], unique = true, name = "idx_food_external")]
)
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nameLocal: String? = null,
    val cuisineTags: List<String> = emptyList(),
    val servingDescription: String,
    val servingGrams: Double,
    val caloriesPerServing: Double,
    val proteinPerServing: Double,
    val carbsPerServing: Double,
    val fatPerServing: Double,
    val fiberPerServing: Double,
    val source: String,
    val externalId: String? = null,
    val isCustom: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
