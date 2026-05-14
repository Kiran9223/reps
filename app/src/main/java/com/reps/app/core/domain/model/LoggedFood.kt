package com.reps.app.core.domain.model

data class LoggedFood(
    val entryId: Long,
    val food: FoodItem,
    val servingMultiplier: Double
) {
    val calories: Double get() = food.caloriesPerServing * servingMultiplier
    val protein: Double get() = food.proteinPerServing * servingMultiplier
    val carbs: Double get() = food.carbsPerServing * servingMultiplier
    val fat: Double get() = food.fatPerServing * servingMultiplier
    val fiber: Double get() = food.fiberPerServing * servingMultiplier
}
