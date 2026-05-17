package com.reps.app.ai

import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable data class ParsedMealToken(val name: String, val quantity: Double, val unit: String)

data class AiMealSuggestion(val foodName: String, val servings: Double, val reason: String)

data class ExerciseAlternative(val name: String, val reason: String)

data class ChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

interface AIRepository {
    suspend fun parseNaturalLanguageMeal(input: String): Result<List<ParsedMealToken>>
    suspend fun getDailyInsight(
        dayLog: DayLog,
        targets: MacroTargets,
        currentWeightKg: Double,
        targetWeightKg: Double,
        workoutDone: Boolean,
        waterMl: Int
    ): Result<String>
    suspend fun getMealSuggestion(
        mealSlot: MealSlot,
        remainingProteinG: Double,
        availableFoods: List<String>
    ): Result<AiMealSuggestion>
    fun getChatResponseStream(history: List<ChatMessage>, userMessage: String, userContext: String): Flow<String>
    suspend fun getShoulderSafeAlternatives(exerciseName: String, muscleGroup: String): Result<List<ExerciseAlternative>>
    suspend fun parseWorkoutPlanText(text: String): Result<ParsedWorkoutPlan>
    suspend fun parseMealPlanText(text: String): Result<ParsedMealPlan>
    fun isAvailable(): Boolean
}
