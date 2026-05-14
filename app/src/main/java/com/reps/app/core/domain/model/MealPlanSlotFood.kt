package com.reps.app.core.domain.model

data class MealPlanSlotFood(
    val slotId: Long,
    val food: FoodItem,
    val servingMultiplier: Double,
    val sortOrder: Int
) {
    val totalCalories: Double get() = food.caloriesPerServing * servingMultiplier
    val totalProtein: Double get() = food.proteinPerServing * servingMultiplier
    val totalCarbs: Double get() = food.carbsPerServing * servingMultiplier
    val totalFat: Double get() = food.fatPerServing * servingMultiplier
}
