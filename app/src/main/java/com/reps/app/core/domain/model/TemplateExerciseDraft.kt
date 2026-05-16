package com.reps.app.core.domain.model

data class TemplateExerciseDraft(
    val exerciseId: Long,
    val targetSets: Int = 3,
    val targetReps: String = "8-12",
    val targetWeightKg: Double? = null
)
