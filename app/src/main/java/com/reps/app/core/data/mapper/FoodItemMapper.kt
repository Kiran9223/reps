package com.reps.app.core.data.mapper

import com.reps.app.core.data.entity.FoodItemEntity
import com.reps.app.core.domain.model.FoodItem
import com.reps.app.core.network.dto.ApiNinjasFoodDto
import com.reps.app.core.network.dto.FoodItemDto
import com.reps.app.core.network.dto.OffProductResponseDto
import com.reps.app.core.network.dto.UsdaFoodDto

private const val USDA_NUTRIENT_ENERGY = 1008
private const val USDA_NUTRIENT_PROTEIN = 1003
private const val USDA_NUTRIENT_FAT = 1004
private const val USDA_NUTRIENT_CARBS = 1005
private const val USDA_NUTRIENT_FIBER = 1079

// Seed DTO → Entity (used by SeedDatabaseWorker)
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

// Entity → Domain model
fun FoodItemEntity.toDomainModel(): FoodItem = FoodItem(
    id = id,
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
    isCustom = isCustom
)

// Domain model → Entity
fun FoodItem.toEntity(): FoodItemEntity = FoodItemEntity(
    id = id,
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
    isCustom = isCustom
)

// USDA DTO → Entity (nutrients are per 100g for Foundation/SR Legacy)
fun UsdaFoodDto.toFoodItemEntity(): FoodItemEntity {
    val nutrients = foodNutrients.associateBy { it.nutrientId }
    return FoodItemEntity(
        name = description,
        servingDescription = "100g",
        servingGrams = 100.0,
        caloriesPerServing = nutrients[USDA_NUTRIENT_ENERGY]?.value ?: 0.0,
        proteinPerServing = nutrients[USDA_NUTRIENT_PROTEIN]?.value ?: 0.0,
        carbsPerServing = nutrients[USDA_NUTRIENT_CARBS]?.value ?: 0.0,
        fatPerServing = nutrients[USDA_NUTRIENT_FAT]?.value ?: 0.0,
        fiberPerServing = nutrients[USDA_NUTRIENT_FIBER]?.value ?: 0.0,
        source = FoodItem.SOURCE_USDA,
        externalId = fdcId.toString(),
        isCustom = false
    )
}

// OFF response → Entity (nutriments are per 100g; scale to serving size)
fun OffProductResponseDto.toFoodItemEntity(barcode: String): FoodItemEntity? {
    val p = product ?: return null
    val n = p.nutriments
    val servingGrams = p.servingQuantity ?: 100.0
    val factor = servingGrams / 100.0
    return FoodItemEntity(
        name = p.productName?.takeIf { it.isNotBlank() } ?: "Unknown Product",
        servingDescription = p.servingSize?.takeIf { it.isNotBlank() }
            ?: "${servingGrams.toInt()}g",
        servingGrams = servingGrams,
        caloriesPerServing = (n.caloriesKcal100g ?: 0.0) * factor,
        proteinPerServing = (n.proteins100g ?: 0.0) * factor,
        carbsPerServing = (n.carbohydrates100g ?: 0.0) * factor,
        fatPerServing = (n.fat100g ?: 0.0) * factor,
        fiberPerServing = (n.fiber100g ?: 0.0) * factor,
        source = FoodItem.SOURCE_OFF,
        externalId = barcode,
        isCustom = false
    )
}

// API Ninjas DTO → Entity
fun ApiNinjasFoodDto.toFoodItemEntity(): FoodItemEntity = FoodItemEntity(
    name = name,
    servingDescription = "${servingSizeG.toInt()}g",
    servingGrams = servingSizeG,
    caloriesPerServing = calories,
    proteinPerServing = proteinG,
    carbsPerServing = carbohydratesTotalG,
    fatPerServing = fatTotalG,
    fiberPerServing = fiberG,
    source = FoodItem.SOURCE_NINJAS,
    externalId = name,
    isCustom = false
)
