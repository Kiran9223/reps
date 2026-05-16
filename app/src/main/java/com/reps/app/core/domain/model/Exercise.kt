package com.reps.app.core.domain.model

data class Exercise(
    val id: Long,
    val name: String,
    val description: String?,
    val muscleGroups: List<String>,
    val equipment: List<String>,
    val isShoulderSafe: Boolean,
    val restrictedMovements: List<String>,
    val instructions: String?,
    val source: String,
    val isCustom: Boolean
)
