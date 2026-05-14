package com.reps.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class FoodItemDto(
    val name: String,
    val nameLocal: String? = null,
    val cuisineTags: List<String> = emptyList(),
    val servingDescription: String,
    val servingGrams: Double,
    val caloriesPerServing: Double,
    val proteinPerServing: Double,
    val carbsPerServing: Double,
    val fatPerServing: Double,
    val fiberPerServing: Double
)
