package com.reps.app.core.domain.model

data class WorkoutSession(
    val id: Long,
    val date: Long,
    val name: String,
    val templateId: Long?,
    val exercises: List<SessionExercise>,
    val durationMinutes: Int?,
    val notes: String?
)

data class SessionExercise(
    val exercise: Exercise,
    val sets: List<CompletedSet>
)

data class CompletedSet(
    val id: Long,
    val setNumber: Int,
    val reps: Int?,
    val weightKg: Double?,
    val isCompleted: Boolean
)
