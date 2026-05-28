package com.reps.app.ai

import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.Exercise
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.model.TemplateExerciseDraft
import com.reps.app.core.domain.model.WorkoutFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable data class ParsedMealToken(val name: String, val quantity: Double, val unit: String)

data class AiMealSuggestion(val foodName: String, val servings: Double, val reason: String)

data class ExerciseAlternative(val name: String, val reason: String)

data class EstimatedNutrition(
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val servingGrams: Double
)

data class EstimatedExercise(
    val muscleGroups: List<String>,
    val equipment: List<String>,
    val isShoulderSafe: Boolean,
    val restrictedMovements: List<String>,
    val description: String
)

data class SuggestedTemplateExercise(
    val exerciseId: Long,
    val targetSets: Int,
    val targetReps: String,
    val targetWeightKg: Double?
)

data class WorkoutTemplateSuggestion(
    val description: String,
    val exercises: List<SuggestedTemplateExercise>
)

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
    suspend fun getQuickWorkoutExercises(
        focus: WorkoutFocus,
        timeBudgetMinutes: Int,
        availableExercises: List<Exercise>
    ): Result<List<TemplateExerciseDraft>>
    suspend fun getShoulderSafeAlternatives(exerciseName: String, muscleGroup: String): Result<List<ExerciseAlternative>>
    suspend fun parseWorkoutPlanText(text: String): Result<ParsedWorkoutPlan>
    suspend fun parseMealPlanText(text: String): Result<ParsedMealPlan>
    suspend fun categorizeGroceryItem(itemName: String): Result<String>
    suspend fun estimateNutrition(foodDescription: String): Result<EstimatedNutrition>
    suspend fun estimateExerciseDetails(exerciseName: String): Result<EstimatedExercise>
    suspend fun estimateWorkoutTemplate(name: String, availableExercises: List<Exercise>): Result<WorkoutTemplateSuggestion>
    fun isAvailable(): Boolean
}
