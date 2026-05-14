package com.reps.app.feature.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.FoodItem
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.repository.FoodRepository
import com.reps.app.core.domain.repository.MealLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    foodRepository: FoodRepository,
    private val mealLogRepository: MealLogRepository
) : ViewModel() {

    private val foodId: Long = checkNotNull(savedStateHandle["foodId"])

    val slotDate: String? = savedStateHandle.get<String>("date")?.takeIf { it.isNotBlank() }
    private val slotName: String? = savedStateHandle.get<String>("slot")?.takeIf { it.isNotBlank() }
    private val mealSlot: MealSlot? = slotName?.let { runCatching { MealSlot.valueOf(it) }.getOrNull() }

    val hasSlotContext: Boolean get() = slotDate != null && mealSlot != null
    val slotDisplayName: String? get() = mealSlot?.displayName

    val food: StateFlow<FoodItem?> = foodRepository.getFoodById(foodId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    private val _multiplier = MutableStateFlow(1.0f)
    val multiplier: StateFlow<Float> = _multiplier.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _addedToLog = MutableStateFlow(false)
    val addedToLog: StateFlow<Boolean> = _addedToLog.asStateFlow()

    fun setMultiplier(value: Float) {
        _multiplier.value = value.coerceIn(0.25f, 5.0f)
    }

    fun addFoodToLog() {
        val date = slotDate ?: return
        val slot = mealSlot ?: return
        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                mealLogRepository.addFoodToLog(
                    date = date,
                    slot = slot,
                    foodItemId = foodId,
                    servings = _multiplier.value.toDouble()
                )
            }
            _isSaving.value = false
            _addedToLog.value = true
        }
    }
}
