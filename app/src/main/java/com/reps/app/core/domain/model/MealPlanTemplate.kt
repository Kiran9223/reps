package com.reps.app.core.domain.model

data class MealPlanTemplate(
    val id: Long,
    val name: String,
    val description: String?,
    val cuisineType: String,
    val days: List<MealPlanDayPlan>,
    val isCustom: Boolean
) {
    val dayLabels: List<String> get() = days.map { it.label }
    val avgDailyCalories: Double
        get() = if (days.isEmpty()) 0.0 else days.map { it.totalCalories }.average()
    val avgDailyProtein: Double
        get() = if (days.isEmpty()) 0.0 else days.map { it.totalProtein }.average()
}
