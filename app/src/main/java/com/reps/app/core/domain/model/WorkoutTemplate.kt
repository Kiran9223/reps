package com.reps.app.core.domain.model

data class WorkoutTemplate(
    val id: Long,
    val name: String,
    val description: String?,
    val isCustom: Boolean,
    val exercises: List<TemplateExercise>
)

data class TemplateExercise(
    val id: Long,
    val exercise: Exercise,
    val targetSets: Int,
    val targetReps: String,
    val targetWeightKg: Double?,
    val sortOrder: Int
)
