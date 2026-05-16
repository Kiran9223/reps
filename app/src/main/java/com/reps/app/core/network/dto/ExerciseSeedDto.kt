package com.reps.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseSeedDto(
    val name: String,
    val description: String? = null,
    val muscleGroups: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val isShoulderSafe: Boolean = true,
    val restrictedMovements: List<String> = emptyList(),
    val instructions: String? = null
)
