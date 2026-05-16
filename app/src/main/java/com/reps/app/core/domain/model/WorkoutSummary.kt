package com.reps.app.core.domain.model

data class WorkoutSummary(
    val id: Long,
    val date: Long,
    val name: String,
    val durationMinutes: Int?,
    val totalSets: Int,
    val totalExercises: Int
)
