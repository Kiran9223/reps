package com.reps.app.ai

import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import kotlinx.coroutines.flow.Flow

class HybridAIRepository(
    private val onDevice: AIRepository,
    private val gemini: AIRepository
) : AIRepository {

    override suspend fun parseNaturalLanguageMeal(input: String) =
        onDevice.parseNaturalLanguageMeal(input)

    override suspend fun getDailyInsight(
        dayLog: DayLog,
        targets: MacroTargets,
        currentWeightKg: Double,
        targetWeightKg: Double,
        workoutDone: Boolean,
        waterMl: Int
    ) = onDevice.getDailyInsight(dayLog, targets, currentWeightKg, targetWeightKg, workoutDone, waterMl)

    override suspend fun getMealSuggestion(
        mealSlot: MealSlot,
        remainingProteinG: Double,
        availableFoods: List<String>
    ) = onDevice.getMealSuggestion(mealSlot, remainingProteinG, availableFoods)

    override fun getChatResponseStream(
        history: List<ChatMessage>,
        userMessage: String,
        userContext: String
    ): Flow<String> = gemini.getChatResponseStream(history, userMessage, userContext)

    override suspend fun getShoulderSafeAlternatives(exerciseName: String, muscleGroup: String) =
        onDevice.getShoulderSafeAlternatives(exerciseName, muscleGroup)

    override suspend fun parseWorkoutPlanText(text: String) = gemini.parseWorkoutPlanText(text)
    override suspend fun parseMealPlanText(text: String) = gemini.parseMealPlanText(text)

    override fun isAvailable(): Boolean = gemini.isAvailable()
}
