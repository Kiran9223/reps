package com.reps.app.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.ai.AIAction
import com.reps.app.ai.AIRepository
import com.reps.app.ai.ChatMessage
import com.reps.app.ai.ExerciseSpec
import com.reps.app.ai.FoodSpec
import com.reps.app.ai.MealSlotSpec
import com.reps.app.core.data.dao.AIChatMessageDao
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import com.reps.app.core.data.entity.AIChatMessageEntity
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.model.ExerciseFilter
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.model.TemplateExerciseDraft
import com.reps.app.core.domain.repository.FoodRepository
import com.reps.app.core.domain.repository.MealLogRepository
import com.reps.app.core.domain.repository.MealPlanRepository
import com.reps.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import javax.inject.Inject

private val actionJson = Json { ignoreUnknownKeys = true; isLenient = true }

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val streamedText: String = "",
    val isStreaming: Boolean = false,
    val isAiAvailable: Boolean = true,
    val pendingAction: AIAction? = null,
    val isConfirmingAction: Boolean = false,
    val actionResult: String? = null
)

@HiltViewModel
class AICoachViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val aiChatMessageDao: AIChatMessageDao,
    private val userPrefs: UserPreferencesDataStore,
    private val mealLogRepository: MealLogRepository,
    private val workoutRepository: WorkoutRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val foodRepository: FoodRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    private val _streamedText = MutableStateFlow("")
    private val _isStreaming = MutableStateFlow(false)
    private val _pendingAction = MutableStateFlow<AIAction?>(null)
    private val _isConfirmingAction = MutableStateFlow(false)
    private val _actionResult = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ChatUiState> = combine(
        aiChatMessageDao.getAll(),
        _inputText,
        _streamedText,
        _isStreaming,
        combine(_pendingAction, _isConfirmingAction, _actionResult) { a, c, r -> Triple(a, c, r) }
    ) { entities, input, streamed, streaming, (action, confirming, result) ->
        ChatUiState(
            messages = entities.map { ChatMessage(it.content, it.isFromUser, it.timestamp) },
            inputText = input,
            streamedText = streamed,
            isStreaming = streaming,
            isAiAvailable = aiRepository.isAvailable(),
            pendingAction = action,
            isConfirmingAction = confirming,
            actionResult = result
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ChatUiState())

    fun onInputChange(text: String) { _inputText.update { text } }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank() || _isStreaming.value) return

        viewModelScope.launch {
            val history = withContext(ioDispatcher) {
                aiChatMessageDao.getRecent(8).reversed()
                    .map { ChatMessage(it.content, it.isFromUser, it.timestamp) }
            }
            withContext(ioDispatcher) {
                aiChatMessageDao.insert(AIChatMessageEntity(content = text, isFromUser = true))
            }
            _inputText.value = ""
            _isStreaming.value = true
            _streamedText.value = ""
            _actionResult.value = null

            val userContext = buildUserContext()
            var fullResponse = ""
            try {
                aiRepository.getChatResponseStream(history, text, userContext)
                    .collect { token ->
                        fullResponse += token
                        _streamedText.value = stripActionBlock(fullResponse)
                    }
            } catch (e: Exception) {
                android.util.Log.e("AICoach", "Chat error: ${e::class.simpleName} — ${e.message}")
                val errorMsg = when {
                    e.message?.contains("quota", ignoreCase = true) == true ||
                    e.message?.contains("limit: 0", ignoreCase = true) == true ->
                        "AI quota exceeded. Please wait a moment and try again."
                    e.message?.contains("rate", ignoreCase = true) == true ->
                        "Too many requests. Please wait a moment and try again."
                    else -> "Something went wrong: ${e::class.simpleName}"
                }
                withContext(ioDispatcher) {
                    aiChatMessageDao.insert(AIChatMessageEntity(content = errorMsg, isFromUser = false))
                }
            } finally {
                if (fullResponse.isNotBlank()) {
                    val displayText = stripActionBlock(fullResponse)
                    withContext(ioDispatcher) {
                        aiChatMessageDao.insert(
                            AIChatMessageEntity(content = displayText, isFromUser = false)
                        )
                    }
                    parseActionBlock(fullResponse)?.let { _pendingAction.value = it }
                }
                _streamedText.value = ""
                _isStreaming.value = false
            }
        }
    }

    fun sendSuggestedPrompt(prompt: String) {
        _inputText.value = prompt
        sendMessage()
    }

    fun dismissAction() {
        _pendingAction.value = null
        _actionResult.value = null
    }

    fun confirmAction() {
        val action = _pendingAction.value ?: return
        _isConfirmingAction.value = true
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                runCatching { executeAction(action) }.getOrElse { "Failed: ${it.message}" }
            }
            _pendingAction.value = null
            _isConfirmingAction.value = false
            _actionResult.value = result
            withContext(ioDispatcher) {
                aiChatMessageDao.insert(AIChatMessageEntity(content = "✓ $result", isFromUser = false))
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch { aiChatMessageDao.clearAll() }
    }

    private suspend fun executeAction(action: AIAction): String = when (action) {
        is AIAction.LogMeal -> executeLogMeal(action)
        is AIAction.UpdateGoal -> executeUpdateGoal(action)
        is AIAction.CreateWorkoutPlan -> executeCreateWorkoutPlan(action)
        is AIAction.CreateMealPlan -> executeCreateMealPlan(action)
    }

    private suspend fun executeLogMeal(action: AIAction.LogMeal): String {
        val slot = runCatching { MealSlot.valueOf(action.slot) }.getOrNull()
            ?: return "Unknown meal slot: ${action.slot}"
        val today = LocalDate.now().toString()
        var logged = 0
        var skipped = 0
        action.foods.forEach { spec ->
            val food = foodRepository.searchFoods(spec.name).first().firstOrNull()
            if (food != null) {
                mealLogRepository.addFoodToLog(today, slot, food.id, spec.servings)
                logged++
            } else {
                skipped++
            }
        }
        return if (skipped == 0) "Logged $logged item(s) to ${slot.displayName}"
        else "Logged $logged item(s) to ${slot.displayName} ($skipped not found in your food library)"
    }

    private suspend fun executeUpdateGoal(action: AIAction.UpdateGoal): String {
        val updates = mutableListOf<String>()
        action.targetWeightKg?.let { newTarget ->
            val current = userPrefs.run {
                val name = name.first()
                val age = age.first()
                val weight = weightKg.first()
                val height = heightCm.first()
                val activity = activityLevel.first()
                val workoutDays = workoutDaysPerWeek.first()
                val shoulder = hasShoulderRestriction.first()
                val restrictions = dietaryRestrictions.first()
                val cuisines = cuisinePreferences.first()
                updateProfile(name, age, weight, newTarget, height, activity, workoutDays,
                    shoulder, restrictions, cuisines)
            }
            updates.add("target weight → ${newTarget}kg")
        }
        action.workoutDaysPerWeek?.let { days ->
            val name = userPrefs.name.first()
            val age = userPrefs.age.first()
            val weight = userPrefs.weightKg.first()
            val target = userPrefs.targetWeightKg.first()
            val height = userPrefs.heightCm.first()
            val activity = userPrefs.activityLevel.first()
            val shoulder = userPrefs.hasShoulderRestriction.first()
            val restrictions = userPrefs.dietaryRestrictions.first()
            val cuisines = userPrefs.cuisinePreferences.first()
            userPrefs.updateProfile(name, age, weight, target, height, activity, days,
                shoulder, restrictions, cuisines)
            updates.add("workout days → $days/week")
        }
        return if (updates.isEmpty()) "No goal changes specified"
        else "Updated: ${updates.joinToString(", ")}"
    }

    private fun exerciseNamesMatch(dbName: String, aiName: String): Boolean {
        if (dbName.contains(aiName, ignoreCase = true) || aiName.contains(dbName, ignoreCase = true)) return true
        val dbNorm = dbName.lowercase().replace(Regex("[^a-z0-9]"), " ").replace(Regex("\\s+"), " ").trim()
        val aiNorm = aiName.lowercase().replace(Regex("[^a-z0-9]"), " ").replace(Regex("\\s+"), " ").trim()
        if (dbNorm.contains(aiNorm) || aiNorm.contains(dbNorm)) return true
        val dbWords = dbNorm.split(" ").filter { it.length > 1 }.toSet()
        val aiWords = aiNorm.split(" ").filter { it.length > 1 }.toSet()
        if (dbWords.isEmpty() || aiWords.isEmpty()) return false
        return dbWords.containsAll(aiWords) || aiWords.containsAll(dbWords)
    }

    private suspend fun executeCreateWorkoutPlan(action: AIAction.CreateWorkoutPlan): String {
        val allExercises = workoutRepository.getExercises(ExerciseFilter()).first()
        val drafts = mutableListOf<TemplateExerciseDraft>()
        val autoCreated = mutableListOf<String>()
        val notFound = mutableListOf<String>()
        action.exercises.forEach { spec ->
            val match = allExercises.firstOrNull { exerciseNamesMatch(it.name, spec.name) }
            if (match != null) {
                drafts.add(TemplateExerciseDraft(exerciseId = match.id, targetSets = spec.sets, targetReps = spec.reps))
            } else {
                val estimated = aiRepository.estimateExerciseDetails(spec.name).getOrNull()
                if (estimated != null) {
                    val newId = workoutRepository.createCustomExercise(spec.name, estimated)
                    drafts.add(TemplateExerciseDraft(exerciseId = newId, targetSets = spec.sets, targetReps = spec.reps))
                    autoCreated.add(spec.name)
                } else {
                    notFound.add(spec.name)
                }
            }
        }
        if (drafts.isEmpty()) return "No exercises could be added. Try again or add them manually."
        workoutRepository.createTemplate(action.title, action.description, drafts)
        return buildString {
            append("Workout plan \"${action.title}\" created with ${drafts.size} exercises.")
            if (autoCreated.isNotEmpty()) append(" Added to your library: ${autoCreated.joinToString()}.")
            if (notFound.isNotEmpty()) append(" Could not add: ${notFound.joinToString()}.")
        }
    }

    private suspend fun executeCreateMealPlan(action: AIAction.CreateMealPlan): String {
        val draftSlots = mutableMapOf<Int, MutableMap<MealSlot, MutableList<Pair<Long, Double>>>>()
        var totalLogged = 0
        var totalSkipped = 0

        action.slots.forEachIndexed { dayIndex, slotSpec ->
            val slot = runCatching { MealSlot.valueOf(slotSpec.slot) }.getOrNull() ?: return@forEachIndexed
            val dayMap = draftSlots.getOrPut(dayIndex) { mutableMapOf() }
            val foodPairs = mutableListOf<Pair<Long, Double>>()
            slotSpec.foods.forEach { spec ->
                val food = foodRepository.searchFoods(spec.name).first().firstOrNull()
                if (food != null) {
                    foodPairs.add(food.id to spec.servings)
                    totalLogged++
                } else {
                    totalSkipped++
                }
            }
            if (foodPairs.isNotEmpty()) dayMap[slot] = foodPairs.toMutableList()
        }

        if (draftSlots.isEmpty()) return "No foods found for the meal plan. Try adding foods to your library first."
        mealPlanRepository.createPlan(action.title, action.description, "Mixed", draftSlots)
        return if (totalSkipped == 0) "Meal plan \"${action.title}\" created"
        else "Meal plan \"${action.title}\" created ($totalSkipped foods not found in library)"
    }

    private fun stripActionBlock(text: String): String =
        text.replace(Regex("<ACTION>[\\s\\S]*?</ACTION>", RegexOption.IGNORE_CASE), "").trim()

    private fun parseActionBlock(text: String): AIAction? {
        val match = Regex("<ACTION>([\\s\\S]*?)</ACTION>", RegexOption.IGNORE_CASE)
            .find(text) ?: return null
        return runCatching {
            val json = match.groupValues[1].trim()
            val obj = actionJson.parseToJsonElement(json).jsonObject
            when (obj["type"]?.jsonPrimitive?.content) {
                "LOG_MEAL" -> AIAction.LogMeal(
                    slot = obj["slot"]?.jsonPrimitive?.content ?: "LUNCH",
                    foods = obj["foods"]?.jsonArray?.map {
                        val f = it.jsonObject
                        FoodSpec(
                            name = f["name"]?.jsonPrimitive?.content ?: "",
                            servings = f["servings"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 1.0
                        )
                    } ?: emptyList()
                )
                "UPDATE_GOAL" -> AIAction.UpdateGoal(
                    targetWeightKg = obj["targetWeightKg"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                    workoutDaysPerWeek = obj["workoutDaysPerWeek"]?.jsonPrimitive?.content?.toIntOrNull()
                )
                "CREATE_WORKOUT_PLAN" -> AIAction.CreateWorkoutPlan(
                    title = obj["title"]?.jsonPrimitive?.content ?: "My Plan",
                    description = obj["description"]?.jsonPrimitive?.content,
                    exercises = obj["exercises"]?.jsonArray?.map {
                        val e = it.jsonObject
                        ExerciseSpec(
                            name = e["name"]?.jsonPrimitive?.content ?: "",
                            sets = e["sets"]?.jsonPrimitive?.content?.toIntOrNull() ?: 3,
                            reps = e["reps"]?.jsonPrimitive?.content ?: "8-12"
                        )
                    } ?: emptyList()
                )
                "CREATE_MEAL_PLAN" -> AIAction.CreateMealPlan(
                    title = obj["title"]?.jsonPrimitive?.content ?: "My Meal Plan",
                    description = obj["description"]?.jsonPrimitive?.content,
                    slots = obj["slots"]?.jsonArray?.map {
                        val s = it.jsonObject
                        MealSlotSpec(
                            slot = s["slot"]?.jsonPrimitive?.content ?: "LUNCH",
                            foods = s["foods"]?.jsonArray?.map { f ->
                                FoodSpec(
                                    name = f.jsonObject["name"]?.jsonPrimitive?.content ?: "",
                                    servings = f.jsonObject["servings"]?.jsonPrimitive?.content
                                        ?.toDoubleOrNull() ?: 1.0
                                )
                            } ?: emptyList()
                        )
                    } ?: emptyList()
                )
                else -> null
            }
        }.getOrNull()
    }

    private suspend fun buildUserContext(): String {
        val name = userPrefs.name.first()
        val weight = userPrefs.weightKg.first()
        val target = userPrefs.targetWeightKg.first()
        val restrictions = userPrefs.dietaryRestrictions.first()
        val shoulder = userPrefs.hasShoulderRestriction.first()
        val workoutDays = userPrefs.workoutDaysPerWeek.first()
        val today = LocalDate.now().toString()
        val dayLog = mealLogRepository.getDayLog(today).first()

        return buildString {
            appendLine("User: $name, weight=${weight}kg, target=${target}kg, workoutDays=$workoutDays/week")
            appendLine("Dietary restrictions: ${restrictions.joinToString().ifBlank { "none" }}")
            appendLine("Shoulder restriction: $shoulder")
            append("Today: ${dayLog.totalCalories.toInt()} kcal eaten, ${dayLog.totalProtein.toInt()}g protein")
        }
    }
}
