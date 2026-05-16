package com.reps.app.core.domain.model

data class GoalProgress(
    val startWeightKg: Double,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val kgRemaining: Double,
    val progressFraction: Float,
    val estimatedWeeksToGoal: Int? = null
) {
    val isGoalReached: Boolean get() = kgRemaining <= 0.0
}
