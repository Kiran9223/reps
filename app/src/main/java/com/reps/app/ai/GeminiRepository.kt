package com.reps.app.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.Exercise
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.model.TemplateExerciseDraft
import com.reps.app.core.domain.model.WorkoutFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val MODEL_NAME = "gemini-2.5-flash"

class GeminiRepository(private val apiKey: String) : AIRepository {

    private val planJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

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
            maxOutputTokens = 2048
        }
    )

    private val parsingModel = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.1f
            maxOutputTokens = 8192
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
                maxOutputTokens = 2048
            },
            systemInstruction = content { text(systemPrompt) }
        )

        val chatHistory = history.map { msg ->
            content(role = if (msg.isFromUser) "user" else "model") { text(msg.content) }
        }

        val chat = model.startChat(history = chatHistory)
        try {
            chat.sendMessageStream(userMessage).collect { chunk ->
                chunk.text?.let { emit(it) }
            }
        } catch (e: ResponseStoppedException) {
            // MAX_TOKENS: partial response already emitted, close cleanly
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

    override suspend fun parseWorkoutPlanText(text: String): Result<ParsedWorkoutPlan> {
        return runCatching {
            val prompt = """
                Parse this workout plan into structured JSON. The text may come from a copied table where columns (Exercise, Sets, Reps, Notes) are merged with no separator — split them correctly using context.

                Input:
                $text

                Respond ONLY with compact JSON (no newlines, no extra spaces), no preamble. Format:
                {"name":"Plan Name","days":[{"label":"Day 1 - Push","exercises":[{"name":"Bench Press","sets":4,"reps":"8-12"}]}]}
            """.trimIndent()
            val response = parsingModel.generateContent(prompt).text
                ?: return Result.failure(Exception("Empty response from Gemini"))
            val cleaned = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            android.util.Log.d("ImportPlan", "Workout raw response: $cleaned")
            planJson.decodeFromString<ParsedWorkoutPlan>(cleaned)
        }
    }

    override suspend fun parseMealPlanText(text: String): Result<ParsedMealPlan> {
        return runCatching {
            val prompt = """
                Parse this meal/diet plan into structured JSON. Map meal times to one of: BREAKFAST, LUNCH, DINNER, SNACKS.
                The text may be messy or copied from a table — extract all food items with their quantities.

                Input:
                $text

                Respond ONLY with compact JSON (no newlines, no extra spaces), no preamble. Format:
                {"name":"Diet Plan","days":[{"label":"Day 1","slots":[{"slot":"BREAKFAST","foods":[{"name":"oats","quantity":1.0,"unit":"cup"}]}]}]}
            """.trimIndent()
            val response = parsingModel.generateContent(prompt).text
                ?: return Result.failure(Exception("Empty response from Gemini"))
            val cleaned = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            android.util.Log.d("ImportPlan", "Meal raw response: $cleaned")
            planJson.decodeFromString<ParsedMealPlan>(cleaned)
        }
    }

    override suspend fun estimateNutrition(foodDescription: String): Result<EstimatedNutrition> {
        return runCatching {
            val prompt = """
                Estimate the nutrition for this food as accurately as possible.
                Food: "$foodDescription"
                Use standard South Indian serving sizes if applicable (e.g. 1 poori ≈ 45g).
                Respond ONLY with valid JSON, no preamble:
                {"calories":250,"protein":5.0,"carbs":38.0,"fat":10.0,"fiber":2.0,"serving_g":180}
            """.trimIndent()
            val json = structuredModel.generateContent(prompt).text?.trim()
                ?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim()
                ?: throw Exception("Empty response")
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.let { j ->
                val obj = j.parseToJsonElement(json).jsonObject
                EstimatedNutrition(
                    calories = obj["calories"]?.jsonPrimitive?.double ?: 0.0,
                    proteinG = obj["protein"]?.jsonPrimitive?.double ?: 0.0,
                    carbsG = obj["carbs"]?.jsonPrimitive?.double ?: 0.0,
                    fatG = obj["fat"]?.jsonPrimitive?.double ?: 0.0,
                    fiberG = obj["fiber"]?.jsonPrimitive?.double ?: 0.0,
                    servingGrams = obj["serving_g"]?.jsonPrimitive?.double ?: 100.0
                )
            }
        }
    }

    override suspend fun estimateExerciseDetails(exerciseName: String): Result<EstimatedExercise> {
        return runCatching {
            val prompt = """
                Provide exercise details for: "$exerciseName"
                Respond ONLY with valid JSON, no preamble:
                {"muscleGroups":["Chest","Triceps"],"equipment":["Barbell","Bench"],"isShoulderSafe":true,"restrictedMovements":[],"description":"A compound push exercise."}
            """.trimIndent()
            val json = structuredModel.generateContent(prompt).text?.trim()
                ?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim()
                ?: throw Exception("Empty response")
            val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val obj = j.parseToJsonElement(json).jsonObject
            EstimatedExercise(
                muscleGroups = obj["muscleGroups"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                equipment = obj["equipment"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                isShoulderSafe = obj["isShoulderSafe"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true,
                restrictedMovements = obj["restrictedMovements"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                description = obj["description"]?.jsonPrimitive?.content ?: ""
            )
        }
    }

    override suspend fun categorizeGroceryItem(itemName: String): Result<String> {
        return runCatching {
            val prompt = """
                Categorize this grocery item into exactly one of: PROTEIN, DAIRY, PRODUCE, FROZEN, PANTRY
                Item: "$itemName"
                Respond with ONLY the category name, nothing else.
            """.trimIndent()
            val response = structuredModel.generateContent(prompt).text?.trim()?.uppercase() ?: "PANTRY"
            val valid = setOf("PROTEIN", "DAIRY", "PRODUCE", "FROZEN", "PANTRY")
            if (response in valid) response else "PANTRY"
        }
    }

    override suspend fun getQuickWorkoutExercises(
        focus: WorkoutFocus,
        timeBudgetMinutes: Int,
        availableExercises: List<Exercise>
    ): Result<List<TemplateExerciseDraft>> {
        val count = when {
            timeBudgetMinutes <= 20 -> 3
            timeBudgetMinutes <= 35 -> 5
            else -> 7
        }
        val sets = if (timeBudgetMinutes >= 45) 4 else 3
        val exerciseList = availableExercises.take(40)
            .joinToString("\n") { "${it.id}|${it.name}|${it.muscleGroups.joinToString(",")}" }
        val prompt = """
            Pick $count exercises for a ${focus.displayName} workout (${timeBudgetMinutes}min).
            Each exercise: $sets sets, 8-12 reps.
            Available exercises (id|name|muscles):
            $exerciseList
            Respond ONLY with valid JSON array, no preamble:
            [{"exerciseId":1,"targetSets":$sets,"targetReps":"8-12"}]
        """.trimIndent()
        return runCatching {
            val response = structuredModel.generateContent(prompt)
            val json = response.text?.trim()
                ?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim()
                ?: return Result.success(emptyList())
            val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val arr = j.parseToJsonElement(json).let {
                it as? kotlinx.serialization.json.JsonArray ?: return Result.success(emptyList())
            }
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                val id = obj["exerciseId"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
                val s = obj["targetSets"]?.jsonPrimitive?.content?.toIntOrNull() ?: sets
                val r = obj["targetReps"]?.jsonPrimitive?.content ?: "8-12"
                TemplateExerciseDraft(exerciseId = id, targetSets = s, targetReps = r)
            }
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
