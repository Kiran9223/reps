package com.reps.app.feature.importplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.ai.AIPrivacyStatus
import com.reps.app.ai.CloudAssistGate
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

enum class ImportPhase {
    IDLE, PARSING, REVIEW, SAVING, SUCCESS, PARTIAL, FAILURE, PARSE_ERROR
}

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
    val statusMessage: String? = null,
    val cloudAssistAvailable: Boolean = false,
    val cloudAssistActive: Boolean = false
)

@HiltViewModel
class ImportPlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val aiRepository: AIRepository,
    aiPrivacyStatus: AIPrivacyStatus,
    private val cloudAssistGate: CloudAssistGate,
    private val workoutRepository: WorkoutRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val foodRepository: FoodRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ImportPlanUiState(
            planType = savedStateHandle.get<String>("type")
                ?.let { runCatching { ImportPlanType.valueOf(it) }.getOrElse { ImportPlanType.WORKOUT } }
                ?: ImportPlanType.WORKOUT,
            cloudAssistAvailable = aiPrivacyStatus.cloudAssistAvailable,
            cloudAssistActive = cloudAssistGate.isCloudActiveSync()
        )
    )
    val uiState: StateFlow<ImportPlanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cloudAssistGate.isCloudActive.collect { active ->
                _uiState.update { it.copy(cloudAssistActive = active) }
            }
        }
    }

    fun onInputChange(text: String) { _uiState.update { it.copy(inputText = text) }  }

    fun onTypeChange(type: ImportPlanType) {
        _uiState.update { it.copy(planType = type, inputText = "") }
    }

    fun parsePlan() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || !_uiState.value.cloudAssistActive) return
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
                            it.copy(phase = ImportPhase.PARSE_ERROR, statusMessage = null)
                        }
                    }
                },
                onFailure = { e ->
                    android.util.Log.e("ImportPlan", "Parse failed: ${e::class.simpleName} — ${e.message}", e)
                    _uiState.update {
                        it.copy(phase = ImportPhase.PARSE_ERROR, statusMessage = null)
                    }
                }
            )
        }
    }

    fun retryInput() { _uiState.update { it.copy(phase = ImportPhase.IDLE, statusMessage = null) } }

    fun backToReview() { _uiState.update { it.copy(phase = ImportPhase.REVIEW, statusMessage = null) } }

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

    fun updateExerciseName(dayIndex: Int, exerciseIndex: Int, name: String) {
        updateExercise(dayIndex, exerciseIndex) { it.copy(name = name) }
    }

    fun updateExerciseSets(dayIndex: Int, exerciseIndex: Int, sets: Int) {
        updateExercise(dayIndex, exerciseIndex) { it.copy(sets = sets.coerceAtLeast(1)) }
    }

    fun updateExerciseReps(dayIndex: Int, exerciseIndex: Int, reps: String) {
        updateExercise(dayIndex, exerciseIndex) { it.copy(reps = reps) }
    }

    private fun updateExercise(
        dayIndex: Int,
        exerciseIndex: Int,
        transform: (ReviewExercise) -> ReviewExercise
    ) {
        _uiState.update { state ->
            val plan = state.reviewWorkout ?: return@update state
            val days = plan.days.mapIndexed { di, day ->
                if (di != dayIndex) return@mapIndexed day
                day.copy(
                    exercises = day.exercises.mapIndexed { ei, ex ->
                        if (ei == exerciseIndex) transform(ex) else ex
                    }
                )
            }
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

    fun updateFoodName(dayIndex: Int, slotIndex: Int, foodIndex: Int, name: String) {
        updateFood(dayIndex, slotIndex, foodIndex) { it.copy(name = name) }
    }

    fun updateFoodQuantity(dayIndex: Int, slotIndex: Int, foodIndex: Int, quantity: Double) {
        updateFood(dayIndex, slotIndex, foodIndex) { it.copy(quantity = quantity.coerceAtLeast(0.0)) }
    }

    private fun updateFood(
        dayIndex: Int,
        slotIndex: Int,
        foodIndex: Int,
        transform: (ReviewFood) -> ReviewFood
    ) {
        _uiState.update { state ->
            val plan = state.reviewMeal ?: return@update state
            val days = plan.days.mapIndexed { di, day ->
                if (di != dayIndex) return@mapIndexed day
                val slots = day.slots.mapIndexed { si, slot ->
                    if (si != slotIndex) return@mapIndexed slot
                    slot.copy(
                        foods = slot.foods.mapIndexed { fi, food ->
                            if (fi == foodIndex) transform(food) else food
                        }
                    )
                }
                day.copy(slots = slots)
            }
            state.copy(reviewMeal = plan.copy(days = days))
        }
    }

    fun canConfirmImport(): Boolean {
        val state = _uiState.value
        state.reviewWorkout?.let { plan ->
            return plan.days.any { it.exercises.isNotEmpty() }
        }
        state.reviewMeal?.let { plan ->
            return plan.days.any { day -> day.slots.any { it.foods.isNotEmpty() } }
        }
        return false
    }

    fun confirmImport() {
        if (!canConfirmImport()) return
        viewModelScope.launch {
            _uiState.update { it.copy(phase = ImportPhase.SAVING) }
            val outcome = withContext(ioDispatcher) {
                runCatching {
                    val state = _uiState.value
                    when {
                        state.reviewWorkout != null -> saveWorkoutPlan(state.reviewWorkout)
                        state.reviewMeal != null -> saveMealPlan(state.reviewMeal)
                        else -> ImportOutcome(ImportPhase.PARTIAL, "Nothing to import.")
                    }
                }.getOrElse {
                    ImportOutcome(ImportPhase.FAILURE, "Import failed. Please try again.")
                }
            }
            _uiState.update {
                it.copy(phase = outcome.phase, statusMessage = outcome.message)
            }
        }
    }

    private data class ImportOutcome(val phase: ImportPhase, val message: String)

    private suspend fun saveWorkoutPlan(plan: ReviewWorkoutPlan): ImportOutcome {
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
        return when {
            created == 0 -> ImportOutcome(
                ImportPhase.PARTIAL,
                "No exercises matched your library. Add exercises first, or edit names in review."
            )
            skipped == 0 -> ImportOutcome(
                ImportPhase.SUCCESS,
                "Imported $created workout template(s)."
            )
            else -> ImportOutcome(
                ImportPhase.SUCCESS,
                "Imported $created template(s). $skipped exercise(s) were not in your library."
            )
        }
    }

    private suspend fun saveMealPlan(plan: ReviewMealPlan): ImportOutcome {
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
        if (draftSlots.isEmpty()) {
            return ImportOutcome(
                ImportPhase.PARTIAL,
                "No foods matched your library. Add foods first, then retry."
            )
        }
        mealPlanRepository.createPlan(plan.name, null, "Imported", draftSlots)
        return if (skipped == 0) {
            ImportOutcome(
                ImportPhase.SUCCESS,
                "Meal plan \"${plan.name}\" created with $logged food(s)."
            )
        } else {
            ImportOutcome(
                ImportPhase.SUCCESS,
                "Created \"${plan.name}\" ($logged foods added, $skipped not in library)."
            )
        }
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
