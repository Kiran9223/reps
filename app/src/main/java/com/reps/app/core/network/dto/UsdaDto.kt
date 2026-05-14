package com.reps.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsdaSearchResponseDto(
    val foods: List<UsdaFoodDto> = emptyList()
)

@Serializable
data class UsdaFoodDto(
    val fdcId: Long,
    val description: String,
    val foodNutrients: List<UsdaFoodNutrientDto> = emptyList()
)

@Serializable
data class UsdaFoodNutrientDto(
    val nutrientId: Int? = null,
    val value: Double? = null
)
