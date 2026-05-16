package com.reps.app.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val MODEL_NAME = "gemini-2.0-flash-lite"

class GeminiRepository(private val apiKey: String) : AIRepository {

    private val chatModel = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            maxOutputTokens = 1024
        }
    )

    private val structuredModel = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.2f
            maxOutputTokens = 512
        }
    )

    override fun getChatResponseStream(
        history: List<ChatMessage>,
        userMessage: String,
        userContext: String
    ): Flow<String> = flow {
        val systemPrompt = buildChatSystemPrompt(userContext)
        val model = GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 1024
            },
            systemInstruction = content { text(systemPrompt) }
        )

        val chatHistory = history.map { msg ->
            content(role = if (msg.isFromUser) "user" else "model") { text(msg.content) }
        }

        val chat = model.startChat(history = chatHistory)
        chat.sendMessageStream(userMessage).collect { chunk ->
            chunk.text?.let { emit(it) }
        }
    }

    override suspend fun parseNaturalLanguageMeal(input: String): Result<List<ParsedMealToken>> {
        return runCatching {
            val prompt = """
                Parse this meal description into individual food items with quantities.
                Input: "$input"
                Respond ONLY with valid JSON array. No preamble.
                Format: [{"name":"food name","quantity":1.0,"unit":"serving"}]
            """.trimIndent()
            val response = structuredModel.generateContent(prompt)
            val json = response.text ?: return Result.success(emptyList())
            val cleaned = json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<List<ParsedMealToken>>(cleaned)
        }
    }

    override suspend fun getDailyInsight(
        dayLog: DayLog,
        targets: MacroTargets,
        currentWeightKg: Double,
        targetWeightKg: Double,
        workoutDone: Boolean,
        waterMl: Int
    ): Result<String> {
        return runCatching {
            val prompt = """
                Give a single motivating insight (2 sentences max) for this fitness day:
                Calories: ${dayLog.totalCalories.toInt()} / ${targets.calories} kcal
                Protein: ${dayLog.totalProtein.toInt()} / ${targets.proteinG}g
                Water: ${waterMl}ml, Workout done: $workoutDone
                Weight: ${currentWeightKg}kg → goal ${targetWeightKg}kg
            """.trimIndent()
            structuredModel.generateContent(prompt).text ?: "Keep pushing — every rep counts."
        }
    }

    override suspend fun getMealSuggestion(
        mealSlot: MealSlot,
        remainingProteinG: Double,
        availableFoods: List<String>
    ): Result<AiMealSuggestion> {
        return runCatching {
            val prompt = """
                Suggest ONE meal for ${mealSlot.displayName} to hit ${remainingProteinG.toInt()}g remaining protein.
                Available foods: ${availableFoods.take(20).joinToString()}.
                Respond ONLY with valid JSON. No preamble.
                Format: {"foodName":"name","servings":1.5,"reason":"brief reason"}
            """.trimIndent()
            val response = structuredModel.generateContent(prompt)
            val json = response.text?.trim()
                ?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim()
                ?: throw Exception("Empty response")
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<AiMealSuggestion>(json)
        }
    }

    override suspend fun getShoulderSafeAlternatives(
        exerciseName: String,
        muscleGroup: String
    ): Result<List<ExerciseAlternative>> {
        return runCatching {
            val prompt = """
                Suggest 3 shoulder-safe alternatives for "$exerciseName" targeting $muscleGroup.
                Respond ONLY with valid JSON array. No preamble.
                Format: [{"name":"exercise","reason":"why it's safe"}]
            """.trimIndent()
            val response = structuredModel.generateContent(prompt)
            val json = response.text?.trim()
                ?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim()
                ?: return Result.success(emptyList())
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<List<ExerciseAlternative>>(json)
        }
    }

    override fun isAvailable(): Boolean = apiKey.isNotBlank()

    private fun buildChatSystemPrompt(userContext: String): String = """
        You are Reps, an expert AI fitness and nutrition coach. Be concise, warm, and actionable.

        $userContext

        When the user asks you to CREATE something in the app (workout plan, meal plan, log a meal,
        or update a goal), include an action at the END of your response using this exact format:

        <ACTION>
        {"type":"ACTION_TYPE", ...fields}
        </ACTION>

        Action types and their JSON schemas:

        LOG_MEAL — log food to a meal slot today:
        {"type":"LOG_MEAL","slot":"BREAKFAST","foods":[{"name":"oats","servings":1.0}]}
        Slot values: BREAKFAST, MORNING_SNACK, LUNCH, AFTERNOON_SNACK, DINNER, EVENING_SNACK

        UPDATE_GOAL — update the user's goals:
        {"type":"UPDATE_GOAL","targetWeightKg":75.0,"workoutDaysPerWeek":4}
        (include only fields that are changing)

        CREATE_WORKOUT_PLAN — create a new workout template:
        {"type":"CREATE_WORKOUT_PLAN","title":"Push Day A","description":"Chest & shoulders","exercises":[{"name":"Bench Press","sets":4,"reps":"8-12"},{"name":"Overhead Press","sets":3,"reps":"10-12"}]}

        CREATE_MEAL_PLAN — create a new weekly meal plan:
        {"type":"CREATE_MEAL_PLAN","title":"High Protein Plan","description":"2000 kcal / 160g protein","slots":[{"slot":"BREAKFAST","foods":[{"name":"oats","servings":1.5},{"name":"eggs","servings":3.0}]},{"slot":"LUNCH","foods":[{"name":"chicken breast","servings":1.5}]}]}

        Only include an <ACTION> block when the user has confirmed they want to create something.
        Never include <ACTION> in a casual conversation response.
        Keep the <ACTION> JSON on a single line inside the tags.
    """.trimIndent()
}
