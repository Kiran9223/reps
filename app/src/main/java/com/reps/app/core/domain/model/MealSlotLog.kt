package com.reps.app.core.domain.model

data class MealSlotLog(
    val logId: Long?,
    val slot: MealSlot,
    val entries: List<LoggedFood> = emptyList()
) {
    val totalCalories: Double get() = entries.sumOf { it.calories }
    val totalProtein: Double get() = entries.sumOf { it.protein }
    val totalCarbs: Double get() = entries.sumOf { it.carbs }
    val totalFat: Double get() = entries.sumOf { it.fat }
}
