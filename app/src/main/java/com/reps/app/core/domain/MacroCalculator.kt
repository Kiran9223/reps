package com.reps.app.core.domain

import com.reps.app.core.domain.model.MacroTargets

object MacroCalculator {

    fun compute(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        activityLevel: String
    ): MacroTargets {
        if (weightKg <= 0 || heightCm <= 0 || age <= 0) return MacroTargets.DEFAULT

        // Mifflin-St Jeor (gender-neutral: average constant omitted)
        val bmr = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        val multiplier = when (activityLevel) {
            "SEDENTARY" -> 1.2
            "LIGHT" -> 1.375
            "ACTIVE" -> 1.725
            "VERY_ACTIVE" -> 1.9
            else -> 1.55 // MODERATE
        }
        val tdee = bmr * multiplier
        val calTarget = (tdee - 500).coerceAtLeast(1200.0) // 500-cal deficit, floor 1200
        val protein = (weightKg * 2.0).toInt().coerceAtLeast(100) // 2 g/kg
        val fat = (calTarget * 0.25 / 9.0).toInt()
        val carbs = ((calTarget - protein * 4.0 - fat * 9.0) / 4.0).toInt().coerceAtLeast(0)

        return MacroTargets(
            calories = calTarget.toInt(),
            proteinG = protein,
            carbsG = carbs,
            fatG = fat,
            waterMl = 2500
        )
    }
}
