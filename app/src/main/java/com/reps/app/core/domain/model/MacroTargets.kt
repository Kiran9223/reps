package com.reps.app.core.domain.model

data class MacroTargets(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val waterMl: Int = 2500
) {
    companion object {
        val DEFAULT = MacroTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67)
    }
}
