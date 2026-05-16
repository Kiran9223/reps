package com.reps.app.core.domain.model

data class ExerciseFilter(
    val query: String = "",
    val shoulderSafeOnly: Boolean = false,
    val muscleGroup: String? = null
)
