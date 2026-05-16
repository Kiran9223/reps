package com.reps.app.feature.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.MealPlanDayPlan
import com.reps.app.core.domain.model.MealPlanTemplate
import com.reps.app.core.domain.repository.MealLogRepository
import com.reps.app.core.domain.repository.MealPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MealPlanUiState(
    val activePlan: MealPlanTemplate? = null,
    val allTemplates: List<MealPlanTemplate> = emptyList(),
    val selectedDayIndex: Int = 0,
    val isEditMode: Boolean = false,
    val showSwitchDialog: Boolean = false,
    val snackMessage: String? = null
)

@HiltViewModel
class MealPlanViewModel @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val mealLogRepository: MealLogRepository
) : ViewModel() {

    private val _selectedDayIndex = MutableStateFlow(0)
    private val _isEditMode = MutableStateFlow(false)
    private val _showSwitchDialog = MutableStateFlow(false)
    private val _snackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MealPlanUiState> = combine(
        mealPlanRepository.getActiveMealPlan(),
        mealPlanRepository.getAllTemplates(),
        _selectedDayIndex,
        _isEditMode,
        _showSwitchDialog
    ) { plan, templates, dayIndex, editMode, showDialog ->
        MealPlanUiState(
            activePlan = plan,
            allTemplates = templates,
            selectedDayIndex = dayIndex.coerceIn(0, maxOf(0, (plan?.days?.size ?: 1) - 1)),
            isEditMode = editMode,
            showSwitchDialog = showDialog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MealPlanUiState())

    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    fun selectDay(index: Int) {
        _selectedDayIndex.value = index
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun showSwitchDialog() {
        _showSwitchDialog.value = true
    }

    fun dismissSwitchDialog() {
        _showSwitchDialog.value = false
    }

    fun switchPlan(templateId: Long) {
        viewModelScope.launch {
            mealPlanRepository.setActivePlan(templateId)
            _showSwitchDialog.value = false
            _selectedDayIndex.value = 0
            _isEditMode.value = false
        }
    }

    fun cloneTemplate(templateId: Long) {
        viewModelScope.launch {
            mealPlanRepository.cloneTemplate(templateId)
            _snackMessage.value = "template_cloned"
        }
    }

    fun updateSlotFood(slotId: Long, foodItemId: Long, servingMultiplier: Double) {
        viewModelScope.launch {
            mealPlanRepository.updateSlotFood(slotId, foodItemId, servingMultiplier)
        }
    }

    fun deletePlan(templateId: Long) {
        viewModelScope.launch {
            mealPlanRepository.deletePlan(templateId)
            _showSwitchDialog.value = false
        }
    }

    fun logTodaysPlan(day: MealPlanDayPlan) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            day.slots.forEach { (slot, foods) ->
                foods.forEach { slotFood ->
                    mealLogRepository.addFoodToLog(today, slot, slotFood.food.id, slotFood.servingMultiplier)
                }
            }
            _snackMessage.value = "plan_logged"
        }
    }

    fun clearSnack() {
        _snackMessage.value = null
    }
}
