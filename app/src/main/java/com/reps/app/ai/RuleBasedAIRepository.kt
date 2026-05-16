package com.reps.app.ai

import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RuleBasedAIRepository : AIRepository {

    override suspend fun parseNaturalLanguageMeal(input: String): Result<List<ParsedMealToken>> =
        Result.failure(UnsupportedOperationException("AI model not available"))

    override suspend fun getDailyInsight(
        dayLog: DayLog,
        targets: MacroTargets,
        currentWeightKg: Double,
        targetWeightKg: Double,
        workoutDone: Boolean,
        waterMl: Int
    ): Result<String> {
        val proteinPct = if (targets.proteinG > 0) dayLog.totalProtein / targets.proteinG else 0.0
        val calPct = if (targets.calories > 0) dayLog.totalCalories / targets.calories else 0.0
        val message = when {
            proteinPct >= 0.9 && workoutDone ->
                "Great work hitting your protein goal and completing a workout today! Keep this momentum going."
            proteinPct >= 0.9 ->
                "You're crushing your protein goal! Add a workout tomorrow to accelerate your progress."
            calPct > 1.1 ->
                "Calories are a bit high today — a lighter dinner will help you stay on track."
            waterMl < targets.waterMl / 2 ->
                "Don't forget to hydrate! Aim for ${targets.waterMl}ml of water today."
            else ->
                "Every meal on target gets you closer to your goal. Stay consistent!"
        }
        return Result.success(message)
    }

    override suspend fun getMealSuggestion(
        mealSlot: MealSlot,
        remainingProteinG: Double,
        availableFoods: List<String>
    ): Result<AiMealSuggestion> {
        val best = availableFoods.firstOrNull()
            ?: return Result.failure(Exception("No foods available"))
        return Result.success(AiMealSuggestion(foodName = best, servings = 1.0, reason = "Good protein source"))
    }

    override fun getChatResponseStream(
        history: List<ChatMessage>,
        userMessage: String,
        userContext: String
    ): Flow<String> = flow {
        emit(
            "AI coach is not available on this device. " +
            "Make sure the model file is at /data/local/tmp/llm/model.task."
        )
    }

    override suspend fun getShoulderSafeAlternatives(
        exerciseName: String,
        muscleGroup: String
    ): Result<List<ExerciseAlternative>> = Result.success(
        listOf(
            ExerciseAlternative("Cable Row", "Works $muscleGroup without overhead stress"),
            ExerciseAlternative("Dumbbell Row", "Shoulder-safe horizontal pulling movement")
        )
    )

    override fun isAvailable(): Boolean = false
}
