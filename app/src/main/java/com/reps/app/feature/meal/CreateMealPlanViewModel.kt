package com.reps.app.feature.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.repository.MealPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DraftSlotFood(
    val foodItemId: Long,
    val foodName: String,
    val servingMultiplier: Double
)

data class CreatePlanState(
    val templateId: Long? = null,
    val name: String = "",
    val description: String = "",
    val cuisineType: String = "South Indian",
    val selectedDayIndex: Int = 0,
    val daySlots: Map<Int, Map<MealSlot, List<DraftSlotFood>>> = buildEmptyDaySlots(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val nameError: Boolean = false,
    val noFoodError: Boolean = false,
    val savedTemplateId: Long? = null
) {
    val hasAnyFood: Boolean
        get() = daySlots.values.any { slotMap -> slotMap.values.any { it.isNotEmpty() } }
}

private fun buildEmptyDaySlots(): Map<Int, Map<MealSlot, List<DraftSlotFood>>> =
    (0..3).associateWith { MealSlot.entries.associateWith { emptyList<DraftSlotFood>() } }

val CUISINE_OPTIONS = listOf(
    "South Indian", "North Indian", "Mediterranean", "Continental", "Mixed"
)

@HiltViewModel
class CreateMealPlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mealPlanRepository: MealPlanRepository
) : ViewModel() {

    private val editTemplateId: Long? =
        savedStateHandle.get<Long>("templateId")?.takeIf { it >= 0L }

    private val _state = MutableStateFlow(CreatePlanState(templateId = editTemplateId))
    val state: StateFlow<CreatePlanState> = _state.asStateFlow()

    init {
        if (editTemplateId != null) loadExistingPlan(editTemplateId)
    }

    private fun loadExistingPlan(templateId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val template = mealPlanRepository.getAllTemplates().first()
                .find { it.id == templateId }
            if (template == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            val daySlots = buildEmptyDaySlots().toMutableMap()
            template.days.forEach { day ->
                val slotMap = (daySlots[day.dayIndex] ?: emptyMap()).toMutableMap()
                day.slots.forEach { (mealSlot, foods) ->
                    slotMap[mealSlot] = foods.map { sf ->
                        DraftSlotFood(
                            foodItemId = sf.food.id,
                            foodName = sf.food.name,
                            servingMultiplier = sf.servingMultiplier
                        )
                    }
                }
                daySlots[day.dayIndex] = slotMap
            }
            _state.value = _state.value.copy(
                name = template.name,
                description = template.description ?: "",
                cuisineType = template.cuisineType,
                daySlots = daySlots,
                isLoading = false
            )
        }
    }

    fun setName(value: String) {
        _state.value = _state.value.copy(name = value, nameError = false)
    }

    fun setDescription(value: String) {
        _state.value = _state.value.copy(description = value)
    }

    fun setCuisineType(value: String) {
        _state.value = _state.value.copy(cuisineType = value)
    }

    fun selectDay(index: Int) {
        _state.value = _state.value.copy(selectedDayIndex = index)
    }

    fun addFoodToSlot(dayIndex: Int, slot: MealSlot, foodItemId: Long, foodName: String, servings: Double) {
        val s = _state.value
        val daySlots = s.daySlots.toMutableMap()
        val slotMap = (daySlots[dayIndex] ?: emptyMap()).toMutableMap()
        slotMap[slot] = (slotMap[slot] ?: emptyList()) + DraftSlotFood(foodItemId, foodName, servings)
        daySlots[dayIndex] = slotMap
        _state.value = s.copy(daySlots = daySlots, noFoodError = false)
    }

    fun removeFoodFromSlot(dayIndex: Int, slot: MealSlot, foodItemId: Long) {
        val s = _state.value
        val daySlots = s.daySlots.toMutableMap()
        val slotMap = (daySlots[dayIndex] ?: emptyMap()).toMutableMap()
        slotMap[slot] = (slotMap[slot] ?: emptyList()).filterNot { it.foodItemId == foodItemId }
        daySlots[dayIndex] = slotMap
        _state.value = s.copy(daySlots = daySlots)
    }

    fun savePlan() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(nameError = true)
            return
        }
        if (!s.hasAnyFood) {
            _state.value = s.copy(noFoodError = true)
            return
        }
        val draftSlots = s.daySlots.mapValues { (_, slotMap) ->
            slotMap.mapValues { (_, foods) -> foods.map { it.foodItemId to it.servingMultiplier } }
        }
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true)
            val savedId = if (s.templateId == null) {
                val newId = mealPlanRepository.createPlan(
                    name = s.name.trim(),
                    description = s.description.trim().takeIf { it.isNotBlank() },
                    cuisineType = s.cuisineType,
                    draftSlots = draftSlots
                )
                mealPlanRepository.setActivePlan(newId)
                newId
            } else {
                mealPlanRepository.updatePlan(
                    templateId = s.templateId,
                    name = s.name.trim(),
                    description = s.description.trim().takeIf { it.isNotBlank() },
                    cuisineType = s.cuisineType,
                    draftSlots = draftSlots
                )
                s.templateId
            }
            _state.value = _state.value.copy(isSaving = false, savedTemplateId = savedId)
        }
    }
}
