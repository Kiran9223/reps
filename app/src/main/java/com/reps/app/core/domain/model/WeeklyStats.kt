package com.reps.app.core.domain.model

data class WeeklyStats(
    val avgCaloriesKcal: Int = 0,
    val avgProteinG: Double = 0.0,
    val workoutsDone: Int = 0,
    val workoutTargetPerWeek: Int = 3,
    val proteinGoalDays: Int = 0
)
