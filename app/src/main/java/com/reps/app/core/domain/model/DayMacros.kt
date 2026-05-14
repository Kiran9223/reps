package com.reps.app.core.domain.model

data class DayMacros(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val targets: MacroTargets = MacroTargets.DEFAULT
) {
    val calorieProgress: Float get() =
        if (targets.calories <= 0) 0f else (calories / targets.calories).toFloat().coerceAtMost(1f)
    val proteinProgress: Float get() =
        if (targets.proteinG <= 0) 0f else (protein / targets.proteinG).toFloat().coerceAtMost(1f)
    val carbsProgress: Float get() =
        if (targets.carbsG <= 0) 0f else (carbs / targets.carbsG).toFloat().coerceAtMost(1f)
    val fatProgress: Float get() =
        if (targets.fatG <= 0) 0f else (fat / targets.fatG).toFloat().coerceAtMost(1f)
}
