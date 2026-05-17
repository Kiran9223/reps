package com.reps.app.feature.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.ai.AIRepository
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.model.FoodItem
import com.reps.app.core.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CustomFoodFormState(
    val name: String = "",
    val servingDescription: String = "",
    val servingGrams: String = "100",
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val fiber: String = "0",
    val selectedCuisineTags: Set<String> = emptySet(),
    val isSubmitting: Boolean = false,
    val isEstimating: Boolean = false,
    val errorMessage: String? = null,
    val savedFoodId: Long? = null
)

@HiltViewModel
class CustomFoodCreationViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val aiRepository: AIRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _formState = MutableStateFlow(CustomFoodFormState())
    val formState: StateFlow<CustomFoodFormState> = _formState.asStateFlow()

    fun onNameChange(v: String) = _formState.update { it.copy(name = v, errorMessage = null) }
    fun onServingDescChange(v: String) = _formState.update { it.copy(servingDescription = v, errorMessage = null) }
    fun onServingGramsChange(v: String) = _formState.update { it.copy(servingGrams = v, errorMessage = null) }
    fun onCaloriesChange(v: String) = _formState.update { it.copy(calories = v, errorMessage = null) }
    fun onProteinChange(v: String) = _formState.update { it.copy(protein = v, errorMessage = null) }
    fun onCarbsChange(v: String) = _formState.update { it.copy(carbs = v, errorMessage = null) }
    fun onFatChange(v: String) = _formState.update { it.copy(fat = v, errorMessage = null) }
    fun onFiberChange(v: String) = _formState.update { it.copy(fiber = v, errorMessage = null) }

    fun onCuisineTagToggle(tag: String) = _formState.update { state ->
        val tags = state.selectedCuisineTags.toMutableSet()
        if (tag in tags) tags.remove(tag) else tags.add(tag)
        state.copy(selectedCuisineTags = tags)
    }

    fun estimateNutrition() {
        val description = _formState.value.name.trim()
        if (description.isBlank()) return
        viewModelScope.launch {
            _formState.update { it.copy(isEstimating = true, errorMessage = null) }
            val result = withContext(ioDispatcher) {
                aiRepository.estimateNutrition(description)
            }
            result.fold(
                onSuccess = { est ->
                    _formState.update { state ->
                        state.copy(
                            isEstimating = false,
                            calories = fmt(est.calories),
                            protein = fmt(est.proteinG),
                            carbs = fmt(est.carbsG),
                            fat = fmt(est.fatG),
                            fiber = fmt(est.fiberG),
                            servingGrams = fmt(est.servingGrams),
                            servingDescription = if (state.servingDescription.isBlank())
                                "${est.servingGrams.toInt()}g" else state.servingDescription
                        )
                    }
                },
                onFailure = { e ->
                    _formState.update {
                        it.copy(isEstimating = false, errorMessage = "Estimation failed: ${e.message}")
                    }
                }
            )
        }
    }

    private fun fmt(value: Double): String {
        return if (value == value.toLong().toDouble()) value.toLong().toString()
        else "%.1f".format(value)
    }

    fun saveFood() {
        val state = _formState.value
        val error = validate(state)
        if (error != null) {
            _formState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            try {
                val food = FoodItem(
                    name = state.name.trim(),
                    servingDescription = state.servingDescription.trim(),
                    servingGrams = state.servingGrams.toDouble(),
                    caloriesPerServing = state.calories.toDouble(),
                    proteinPerServing = state.protein.toDouble(),
                    carbsPerServing = state.carbs.toDouble(),
                    fatPerServing = state.fat.toDouble(),
                    fiberPerServing = state.fiber.toDoubleOrNull() ?: 0.0,
                    cuisineTags = state.selectedCuisineTags.toList(),
                    source = FoodItem.SOURCE_CUSTOM,
                    isCustom = true
                )
                val id = foodRepository.createCustomFood(food)
                _formState.update { it.copy(isSubmitting = false, savedFoodId = id) }
            } catch (e: Exception) {
                _formState.update { it.copy(isSubmitting = false, errorMessage = "Failed to save food") }
            }
        }
    }

    private fun validate(state: CustomFoodFormState): String? {
        if (state.name.isBlank()) return "Food name is required"
        if (state.servingDescription.isBlank()) return "Serving description is required"
        if (state.servingGrams.toDoubleOrNull() == null) return "Invalid serving size"
        if (state.calories.toDoubleOrNull() == null) return "Invalid calories"
        if (state.protein.toDoubleOrNull() == null) return "Invalid protein"
        if (state.carbs.toDoubleOrNull() == null) return "Invalid carbs"
        if (state.fat.toDoubleOrNull() == null) return "Invalid fat"
        return null
    }
}
