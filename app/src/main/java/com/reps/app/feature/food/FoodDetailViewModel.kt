package com.reps.app.feature.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.FoodItem
import com.reps.app.core.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    foodRepository: FoodRepository
) : ViewModel() {

    private val foodId: Long = checkNotNull(savedStateHandle["foodId"])

    val food: StateFlow<FoodItem?> = foodRepository.getFoodById(foodId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    private val _multiplier = MutableStateFlow(1.0f)
    val multiplier: StateFlow<Float> = _multiplier.asStateFlow()

    fun setMultiplier(value: Float) {
        _multiplier.value = value.coerceIn(0.25f, 5.0f)
    }
}
