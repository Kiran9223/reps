package com.reps.app.ai

sealed class AIAction {
    data class LogMeal(
        val slot: String,
        val foods: List<FoodSpec>
    ) : AIAction()

    data class UpdateGoal(
        val targetWeightKg: Double? = null,
        val workoutDaysPerWeek: Int? = null
    ) : AIAction()

    data class CreateWorkoutPlan(
        val title: String,
        val description: String?,
        val exercises: List<ExerciseSpec>
    ) : AIAction()

    data class CreateMealPlan(
        val title: String,
        val description: String?,
        val slots: List<MealSlotSpec>
    ) : AIAction()
}

data class FoodSpec(val name: String, val servings: Double)
data class ExerciseSpec(val name: String, val sets: Int, val reps: String)
data class MealSlotSpec(val slot: String, val foods: List<FoodSpec>)
