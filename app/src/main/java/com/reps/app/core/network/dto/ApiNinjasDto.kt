package com.reps.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiNinjasFoodDto(
    val name: String,
    val calories: Double = 0.0,
    @SerialName("serving_size_g") val servingSizeG: Double = 100.0,
    @SerialName("fat_total_g") val fatTotalG: Double = 0.0,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carbohydrates_total_g") val carbohydratesTotalG: Double = 0.0,
    @SerialName("fiber_g") val fiberG: Double = 0.0
)
