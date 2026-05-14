package com.reps.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OffProductResponseDto(
    val status: Int = 0,
    val product: OffProductDto? = null
)

@Serializable
data class OffProductDto(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("serving_quantity") val servingQuantity: Double? = null,
    val nutriments: OffNutrimentsDto = OffNutrimentsDto()
)

@Serializable
data class OffNutrimentsDto(
    @SerialName("energy-kcal_100g") val caloriesKcal100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null
)
