package com.reps.app.core.data.mapper

import com.reps.app.core.data.entity.FoodItemEntity
import com.reps.app.core.network.dto.FoodItemDto

fun FoodItemDto.toEntity(source: String = "ASSET", externalId: String? = null): FoodItemEntity =
    FoodItemEntity(
        name = name,
        nameLocal = nameLocal,
        cuisineTags = cuisineTags,
        servingDescription = servingDescription,
        servingGrams = servingGrams,
        caloriesPerServing = caloriesPerServing,
        proteinPerServing = proteinPerServing,
        carbsPerServing = carbsPerServing,
        fatPerServing = fatPerServing,
        fiberPerServing = fiberPerServing,
        source = source,
        externalId = externalId,
        isCustom = false
    )
