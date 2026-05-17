package com.reps.app.feature.importplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.ai.AIRepository
import com.reps.app.ai.ImportPlanType
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.model.ExerciseFilter
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.model.TemplateExerciseDraft
import com.reps.app.core.domain.repository.FoodRepository
import com.reps.app.core.domain.repository.MealPlanRepository
import com.reps.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class ImportPhase { IDLE, PARSING, REVIEW, SAVING, DONE, ERROR }

data class ReviewExercise(val name: String, val sets: Int, val reps: String)
data class ReviewWorkoutDay(val label: String, val exercises: List<ReviewExercise>)
data class ReviewWorkoutPlan(val name: String, val days: List<ReviewWorkoutDay>)

data class ReviewFood(val name: String, val quantity: Double, val unit: String)
data class ReviewMealSlot(val slotKey: String, val displayName: String, val foods: List<ReviewFood>)
data class ReviewMealDay(val label: String, val slots: List<ReviewMealSlot>)
data class ReviewMealPlan(val name: String, val days: List<ReviewMealDay>)

data class ImportPlanUiState(
    val planType: ImportPlanType = ImportPlanType.WORKOUT,
    val inputText: String = "",
    val phase: ImportPhase = ImportPhase.IDLE,
    val reviewWorkout: ReviewWorkoutPlan? = null,
    val reviewMeal: ReviewMealPlan? = null,
    val statusMessage: String? = null
)

@HiltViewModel
class ImportPlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val aiRepository: AIRepository,
    private val workoutRepository: WorkoutRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val foodRepository: FoodRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ImportPlanUiState(
            planType = savedStateHandle.get<String>("type")
                ?.let { runCatching { ImportPlanType.valueOf(it) }.getOrElse { ImportPlanType.WORKOUT } }
                ?: ImportPlanType.WORKOUT
        )
    )
    val uiState: StateFlow<ImportPlanUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) { _uiState.update { it.copy(inputText = text) }  }

    fun onTypeChange(type: ImportPlanType) { _uiState.update { it.copy(planType = type) } }

    fun parsePlan() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(phase = ImportPhase.PARSING) }
            val result = withContext(ioDispatcher) {
                if (_uiState.value.planType == ImportPlanType.WORKOUT)
                    aiRepository.parseWorkoutPlanText(text)
                else
                    aiRepository.parseMealPlanText(text)
            }
            result.fold(
                onSuccess = { parsed ->
                    when (parsed) {
                        is com.reps.app.ai.ParsedWorkoutPlan -> _uiState.update {
                            it.copy(
                                phase = ImportPhase.REVIEW,
                                reviewWorkout = parsed.toReview(),
                                reviewMeal = null
                            )
                        }
                        is com.reps.app.ai.ParsedMealPlan -> _uiState.update {
                            it.copy(
                                phase = ImportPhase.REVIEW,
                                reviewMeal = parsed.toReview(),
                                reviewWorkout = null
                            )
                        }
                        else -> _uiState.update {
                            it.copy(phase = ImportPhase.ERROR, statusMessage = "Could not parse plan. Try re-pasting the text.")
                        }
                    }
                },
                onFailure = { e ->
                    android.util.Log.e("ImportPlan", "Parse failed: ${e::class.simpleName} — ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            phase = ImportPhase.ERROR,
                            statusMessage = if (e.message?.contains("not available") == true)
                                "On-device AI is not available. Make sure the model is installed."
                            else
                                "Failed to parse: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun retryInput() { _uiState.update { it.copy(phase = ImportPhase.IDLE, statusMessage = null) } }

    // Workout review edits
    fun updateWorkoutPlanName(name: String) {
        _uiState.update { it.copy(reviewWorkout = it.reviewWorkout?.copy(name = name)) }
    }

    fun removeWorkoutDay(dayIndex: Int) {
        _uiState.update { state ->
            val plan = state.reviewWorkout ?: return@update state
            state.copy(reviewWorkout = plan.copy(days = plan.days.toMutableList().also { it.removeAt(dayIndex) }))
        }
    }

    fun removeExercise(dayIndex: Int, exerciseIndex: Int) {
        _uiState.update { state ->
            val plan = state.reviewWorkout ?: return@update state
            val days = plan.days.mapIndexed { i, day ->
                if (i == dayIndex) day.copy(exercises = day.exercises.toMutableList().also { it.removeAt(exerciseIndex) })
                else day
            }.filter { it.exercises.isNotEmpty() }
            state.copy(reviewWorkout = plan.copy(days = days))
        }
    }

    // Meal review edits
    fun updateMealPlanName(name: String) {
        _uiState.update { it.copy(reviewMeal = it.reviewMeal?.copy(name = name)) }
    }

    fun removeMealDay(dayIndex: Int) {
        _uiState.update { state ->
            val plan = state.reviewMeal ?: return@update state
            state.copy(reviewMeal = plan.copy(days = plan.days.toMutableList().also { it.removeAt(dayIndex) }))
        }
    }

    fun removeMealFood(dayIndex: Int, slotIndex: Int, foodIndex: Int) {
        _uiState.update { state ->
            val plan = state.reviewMeal ?: return@update state
            val days = plan.days.mapIndexed { di, day ->
                if (di != dayIndex) return@mapIndexed day
                val slots = day.slots.mapIndexed { si, slot ->
                    if (si != slotIndex) return@mapIndexed slot
                    slot.copy(foods = slot.foods.toMutableList().also { it.removeAt(foodIndex) })
                }.filter { it.foods.isNotEmpty() }
                day.copy(slots = slots)
            }.filter { it.slots.isNotEmpty() }
            state.copy(reviewMeal = plan.copy(days = days))
        }
    }

    fun confirmImport() {
        viewModelScope.launch {
            _uiState.update { it.copy(phase = ImportPhase.SAVING) }
            val message = withContext(ioDispatcher) {
                runCatching {
                    val state = _uiState.value
                    if (state.reviewWorkout != null) saveWorkoutPlan(state.reviewWorkout)
                    else if (state.reviewMeal != null) saveMealPlan(state.reviewMeal)
                    else "Nothing to import."
                }.getOrElse { "Import failed: ${it.message}" }
            }
            _uiState.update { it.copy(phase = ImportPhase.DONE, statusMessage = message) }
        }
    }

    private suspend fun saveWorkoutPlan(plan: ReviewWorkoutPlan): String {
        val allExercises = workoutRepository.getExercises(ExerciseFilter()).first()
        var created = 0
        var skipped = 0
        plan.days.forEach { day ->
            val drafts = day.exercises.mapNotNull { ex ->
                val match = allExercises.firstOrNull {
                    it.name.contains(ex.name, ignoreCase = true) ||
                        ex.name.contains(it.name, ignoreCase = true)
                }
                if (match != null) TemplateExerciseDraft(match.id, ex.sets, ex.reps)
                else { skipped++; null }
            }
            if (drafts.isNotEmpty()) {
                val templateName = if (plan.days.size == 1) plan.name else "${plan.name} — ${day.label}"
                workoutRepository.createTemplate(templateName, plan.name.takeIf { plan.days.size > 1 }, drafts)
                created++
            }
        }
        return if (skipped == 0) "Created $created workout template(s)"
        else "Created $created template(s), $skipped exercise(s) not in library"
    }

    private suspend fun saveMealPlan(plan: ReviewMealPlan): String {
        val draftSlots = mutableMapOf<Int, MutableMap<MealSlot, MutableList<Pair<Long, Double>>>>()
        var logged = 0
        var skipped = 0
        plan.days.forEachIndexed { dayIndex, day ->
            day.slots.forEach { slot ->
                val mealSlot = runCatching { MealSlot.valueOf(slot.slotKey) }.getOrNull() ?: return@forEach
                slot.foods.forEach { food ->
                    val found = foodRepository.searchFoods(food.name).first().firstOrNull()
                    if (found != null) {
                        draftSlots.getOrPut(dayIndex) { mutableMapOf() }
                            .getOrPut(mealSlot) { mutableListOf() }
                            .add(found.id to food.quantity)
                        logged++
                    } else {
                        skipped++
                    }
                }
            }
        }
        if (draftSlots.isEmpty()) return "No foods matched your library. Add foods first, then retry."
        mealPlanRepository.createPlan(plan.name, null, "Imported", draftSlots)
        return if (skipped == 0) "Meal plan \"${plan.name}\" created with $logged food(s)"
        else "Created \"${plan.name}\" ($logged foods added, $skipped not in library)"
    }

    // Mappers from AI parsed models to review models
    private fun com.reps.app.ai.ParsedWorkoutPlan.toReview() = ReviewWorkoutPlan(
        name = name,
        days = days.map { day ->
            ReviewWorkoutDay(
                label = day.label,
                exercises = day.exercises.map { ReviewExercise(it.name, it.sets, it.reps) }
            )
        }
    )

    private fun com.reps.app.ai.ParsedMealPlan.toReview() = ReviewMealPlan(
        name = name,
        days = days.map { day ->
            ReviewMealDay(
                label = day.label,
                slots = day.slots.map { slot ->
                    ReviewMealSlot(
                        slotKey = slot.slot,
                        displayName = runCatching { MealSlot.valueOf(slot.slot).displayName }
                            .getOrElse { slot.slot.lowercase().replaceFirstChar { it.uppercase() } },
                        foods = slot.foods.map { ReviewFood(it.name, it.quantity, it.unit) }
                    )
                }
            )
        }
    )
}
