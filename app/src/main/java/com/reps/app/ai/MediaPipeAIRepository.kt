package com.reps.app.ai

import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MediaPipeAIRepository(
    private val llmInference: LlmInference
) : AIRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun parseNaturalLanguageMeal(input: String): Result<List<ParsedMealToken>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val prompt = """
You are a nutrition assistant. Extract food items from this input: '$input'
The user prefers South Indian food. No beef or pork.
Respond ONLY with valid JSON array, no preamble:
[{"name": "idli", "quantity": 2, "unit": "piece"}]
                """.trimIndent()
                val response = llmInference.generateResponse(prompt)
                parseTokenArray(response)
            }
        }

    override suspend fun getDailyInsight(
        dayLog: DayLog,
        targets: MacroTargets,
        currentWeightKg: Double,
        targetWeightKg: Double,
        workoutDone: Boolean,
        waterMl: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = """
Fitness coach giving a daily check-in. Be encouraging, max 2 sentences.
User goal: lose weight from ${currentWeightKg}kg to ${targetWeightKg}kg.
Today: ${dayLog.totalCalories.toInt()}/${targets.calories}kcal, protein ${dayLog.totalProtein.toInt()}/${targets.proteinG}g, water ${waterMl}/${targets.waterMl}ml, workout done: $workoutDone.
            """.trimIndent()
            llmInference.generateResponse(prompt).trim()
        }
    }

    override suspend fun getMealSuggestion(
        mealSlot: MealSlot,
        remainingProteinG: Double,
        availableFoods: List<String>
    ): Result<AiMealSuggestion> = withContext(Dispatchers.IO) {
        runCatching {
            val foods = availableFoods.take(15).joinToString(", ")
            val prompt = """
You are a South Indian nutrition coach. No beef or pork.
User needs a ${mealSlot.displayName} meal with approximately ${remainingProteinG.toInt()}g protein.
Available foods: $foods
Respond ONLY with valid JSON, no preamble:
{"foodName": "chicken breast", "servings": 1.5, "reason": "hits protein goal"}
            """.trimIndent()
            val response = llmInference.generateResponse(prompt)
            parseSuggestion(response)
        }
    }

    override fun getChatResponseStream(
        history: List<ChatMessage>,
        userMessage: String,
        userContext: String
    ): Flow<String> = callbackFlow {
        val historyText = history.takeLast(8).joinToString("\n") { msg ->
            if (msg.isFromUser) "User: ${msg.content}" else "Reps: ${msg.content}"
        }
        val prompt = buildString {
            append(userContext)
            append("\n\n")
            if (historyText.isNotBlank()) {
                append(historyText)
                append("\n")
            }
            append("User: $userMessage\nReps:")
        }
        llmInference.generateResponseAsync(prompt) { partial, done ->
            if (!partial.isNullOrEmpty()) trySend(partial)
            if (done) close()
        }
        awaitClose()
    }

    override suspend fun getShoulderSafeAlternatives(
        exerciseName: String,
        muscleGroup: String
    ): Result<List<ExerciseAlternative>> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = """
Suggest 2 shoulder-safe alternative exercises for: $exerciseName
Target muscle group: $muscleGroup. Available equipment: gym machines.
No overhead movements. Respond ONLY with JSON:
[{"name": "...", "reason": "..."}]
            """.trimIndent()
            val response = llmInference.generateResponse(prompt)
            parseAlternatives(response)
        }
    }

    override suspend fun parseWorkoutPlanText(text: String): Result<ParsedWorkoutPlan> =
        withContext(Dispatchers.IO) {
            runCatching {
                val prompt = """
Parse this workout plan. Extract days with their exercises, sets and reps.
Text: "$text"
Respond ONLY with JSON, no preamble:
{"name":"Plan Name","days":[{"label":"Day 1 - Push","exercises":[{"name":"Bench Press","sets":4,"reps":"8-12"}]}]}
                """.trimIndent()
                parseWorkoutPlan(llmInference.generateResponse(prompt))
            }
        }

    override suspend fun parseMealPlanText(text: String): Result<ParsedMealPlan> =
        withContext(Dispatchers.IO) {
            runCatching {
                val prompt = """
Parse this meal plan. Map meal times to: BREAKFAST, LUNCH, DINNER, or SNACKS.
Text: "$text"
Respond ONLY with JSON, no preamble:
{"name":"Diet Plan","days":[{"label":"Day 1","slots":[{"slot":"BREAKFAST","foods":[{"name":"oats","quantity":1.0,"unit":"cup"}]}]}]}
                """.trimIndent()
                parseMealPlan(llmInference.generateResponse(prompt))
            }
        }

    override fun isAvailable(): Boolean = true

    private fun parseTokenArray(response: String): List<ParsedMealToken> {
        val jsonStr = extractJson(response, '[', ']')
        val array = json.parseToJsonElement(jsonStr) as JsonArray
        return array.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                ParsedMealToken(
                    name = obj["name"]?.jsonPrimitive?.content.orEmpty(),
                    quantity = obj["quantity"]?.jsonPrimitive?.doubleOrNull ?: 1.0,
                    unit = obj["unit"]?.jsonPrimitive?.content ?: "serving"
                )
            }.getOrNull()
        }.filter { it.name.isNotBlank() }
    }

    private fun parseSuggestion(response: String): AiMealSuggestion {
        val jsonStr = extractJson(response, '{', '}')
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        return AiMealSuggestion(
            foodName = obj["foodName"]?.jsonPrimitive?.content.orEmpty(),
            servings = obj["servings"]?.jsonPrimitive?.doubleOrNull ?: 1.0,
            reason = obj["reason"]?.jsonPrimitive?.content.orEmpty()
        )
    }

    private fun parseAlternatives(response: String): List<ExerciseAlternative> {
        val jsonStr = extractJson(response, '[', ']')
        val array = json.parseToJsonElement(jsonStr) as JsonArray
        return array.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                ExerciseAlternative(
                    name = obj["name"]?.jsonPrimitive?.content.orEmpty(),
                    reason = obj["reason"]?.jsonPrimitive?.content.orEmpty()
                )
            }.getOrNull()
        }.filter { it.name.isNotBlank() }
    }

    private fun parseWorkoutPlan(response: String): ParsedWorkoutPlan {
        val jsonStr = extractJson(response, '{', '}')
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        val name = obj["name"]?.jsonPrimitive?.content ?: "Imported Workout Plan"
        val days = obj["days"]?.jsonArray?.mapNotNull { dayEl ->
            runCatching {
                val dayObj = dayEl.jsonObject
                val exercises = dayObj["exercises"]?.jsonArray?.mapNotNull { exEl ->
                    runCatching {
                        val exObj = exEl.jsonObject
                        ParsedExerciseItem(
                            name = exObj["name"]?.jsonPrimitive?.content.orEmpty(),
                            sets = exObj["sets"]?.jsonPrimitive?.content?.toIntOrNull() ?: 3,
                            reps = exObj["reps"]?.jsonPrimitive?.content ?: "10"
                        )
                    }.getOrNull()
                }?.filter { it.name.isNotBlank() } ?: emptyList()
                ParsedWorkoutDay(
                    label = dayObj["label"]?.jsonPrimitive?.content ?: "Day",
                    exercises = exercises
                )
            }.getOrNull()
        }?.filter { it.exercises.isNotEmpty() } ?: emptyList()
        return ParsedWorkoutPlan(name, days)
    }

    private fun parseMealPlan(response: String): ParsedMealPlan {
        val jsonStr = extractJson(response, '{', '}')
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        val name = obj["name"]?.jsonPrimitive?.content ?: "Imported Meal Plan"
        val days = obj["days"]?.jsonArray?.mapNotNull { dayEl ->
            runCatching {
                val dayObj = dayEl.jsonObject
                val slots = dayObj["slots"]?.jsonArray?.mapNotNull { slotEl ->
                    runCatching {
                        val slotObj = slotEl.jsonObject
                        val foods = slotObj["foods"]?.jsonArray?.mapNotNull { foodEl ->
                            runCatching {
                                val fObj = foodEl.jsonObject
                                ParsedMealToken(
                                    name = fObj["name"]?.jsonPrimitive?.content.orEmpty(),
                                    quantity = fObj["quantity"]?.jsonPrimitive?.doubleOrNull ?: 1.0,
                                    unit = fObj["unit"]?.jsonPrimitive?.content ?: "serving"
                                )
                            }.getOrNull()
                        }?.filter { it.name.isNotBlank() } ?: emptyList()
                        ParsedMealSlotItem(
                            slot = slotObj["slot"]?.jsonPrimitive?.content ?: "BREAKFAST",
                            foods = foods
                        )
                    }.getOrNull()
                }?.filter { it.foods.isNotEmpty() } ?: emptyList()
                ParsedMealDay(
                    label = dayObj["label"]?.jsonPrimitive?.content ?: "Day 1",
                    slots = slots
                )
            }.getOrNull()
        }?.filter { it.slots.isNotEmpty() } ?: emptyList()
        return ParsedMealPlan(name, days)
    }

    private fun extractJson(text: String, open: Char, close: Char): String {
        val start = text.indexOf(open)
        val end = text.lastIndexOf(close)
        return if (start != -1 && end > start) text.substring(start, end + 1) else text.trim()
    }
}
