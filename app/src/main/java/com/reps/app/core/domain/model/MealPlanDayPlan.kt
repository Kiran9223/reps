package com.reps.app.core.domain.model

data class MealPlanDayPlan(
    val dayIndex: Int,
    val label: String,
    val slots: Map<MealSlot, List<MealPlanSlotFood>>
) {
    val totalCalories: Double get() = slots.values.flatten().sumOf { it.totalCalories }
    val totalProtein: Double get() = slots.values.flatten().sumOf { it.totalProtein }
    val totalCarbs: Double get() = slots.values.flatten().sumOf { it.totalCarbs }
    val totalFat: Double get() = slots.values.flatten().sumOf { it.totalFat }
}
