package com.reps.app.ai

import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.Exercise
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.model.TemplateExerciseDraft
import com.reps.app.core.domain.model.WorkoutFocus
import kotlinx.coroutines.flow.Flow

class HybridAIRepository(
    private val onDevice: AIRepository,
    private val gemini: AIRepository,
    private val cloudAssistGate: CloudAssistGate
) : AIRepository {

    private suspend fun cloudActive(): Boolean = cloudAssistGate.isCloudActiveNow()

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
    ): Flow<String> {
        if (!cloudAssistGate.isCloudActiveSync()) {
            return kotlinx.coroutines.flow.flow {
                throw IllegalStateException("Cloud assist is off")
            }
        }
        return gemini.getChatResponseStream(history, userMessage, userContext)
    }

    override suspend fun getShoulderSafeAlternatives(exerciseName: String, muscleGroup: String) =
        onDevice.getShoulderSafeAlternatives(exerciseName, muscleGroup)

    override suspend fun parseWorkoutPlanText(text: String) =
        if (cloudActive()) gemini.parseWorkoutPlanText(text)
        else Result.failure(IllegalStateException("Cloud assist is off"))

    override suspend fun parseMealPlanText(text: String) =
        if (cloudActive()) gemini.parseMealPlanText(text)
        else Result.failure(IllegalStateException("Cloud assist is off"))
    override suspend fun categorizeGroceryItem(itemName: String) = onDevice.categorizeGroceryItem(itemName)
    override suspend fun estimateNutrition(foodDescription: String) = onDevice.estimateNutrition(foodDescription)
    override suspend fun estimateExerciseDetails(exerciseName: String): Result<EstimatedExercise> {
        if (cloudActive()) {
            val result = gemini.estimateExerciseDetails(exerciseName)
            if (result.isSuccess) return result
        }
        return onDevice.estimateExerciseDetails(exerciseName)
    }

    override suspend fun estimateWorkoutTemplate(name: String, availableExercises: List<Exercise>) =
        if (cloudActive()) gemini.estimateWorkoutTemplate(name, availableExercises)
        else Result.failure(IllegalStateException("Cloud assist is off"))

    override suspend fun getQuickWorkoutExercises(
        focus: WorkoutFocus,
        timeBudgetMinutes: Int,
        availableExercises: List<Exercise>
    ): Result<List<TemplateExerciseDraft>> =
        onDevice.getQuickWorkoutExercises(focus, timeBudgetMinutes, availableExercises)

    override fun isAvailable(): Boolean = gemini.isAvailable()
}
