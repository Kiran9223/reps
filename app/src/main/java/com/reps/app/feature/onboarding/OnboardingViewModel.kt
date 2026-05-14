package com.reps.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }

data class OnboardingState(
    val currentStep: Int = 1,
    val name: String = "",
    val age: String = "",
    val weightKg: String = "",
    val targetWeightKg: String = "",
    val heightCm: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val workoutDaysPerWeek: Int = 3,
    val hasShoulderRestriction: Boolean = false,
    val selectedDietaryRestrictions: Set<String> = emptySet(),
    val selectedCuisinePreferences: Set<String> = emptySet(),
    val isCompleting: Boolean = false
) {
    val totalSteps: Int get() = 5
    val canGoNext: Boolean get() = when (currentStep) {
        1 -> true
        2 -> name.isNotBlank() && age.toIntOrNull()?.let { it > 0 } == true
        3 -> weightKg.toDoubleOrNull()?.let { it > 0 } == true
                && targetWeightKg.toDoubleOrNull()?.let { it > 0 } == true
                && heightCm.toDoubleOrNull()?.let { it > 0 } == true
        4 -> true
        5 -> true
        else -> false
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val appSettingsDataStore: AppSettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    companion object {
        val DIETARY_RESTRICTION_KEYS = listOf(
            "vegetarian", "vegan", "no-beef", "no-pork",
            "no-seafood", "gluten-free", "dairy-free"
        )
        val CUISINE_PREFERENCE_KEYS = listOf(
            "south-indian", "north-indian", "mediterranean",
            "continental", "chinese"
        )
    }

    fun updateName(value: String) = _state.update { it.copy(name = value) }
    fun updateAge(value: String) = _state.update { it.copy(age = value) }
    fun updateWeightKg(value: String) = _state.update { it.copy(weightKg = value) }
    fun updateTargetWeightKg(value: String) = _state.update { it.copy(targetWeightKg = value) }
    fun updateHeightCm(value: String) = _state.update { it.copy(heightCm = value) }
    fun updateActivityLevel(level: ActivityLevel) = _state.update { it.copy(activityLevel = level) }
    fun updateWorkoutDays(days: Int) = _state.update { it.copy(workoutDaysPerWeek = days) }
    fun toggleShoulderRestriction() = _state.update { it.copy(hasShoulderRestriction = !it.hasShoulderRestriction) }

    fun toggleDietaryRestriction(key: String) = _state.update {
        val updated = it.selectedDietaryRestrictions.toMutableSet()
        if (key in updated) updated.remove(key) else updated.add(key)
        it.copy(selectedDietaryRestrictions = updated)
    }

    fun toggleCuisinePreference(key: String) = _state.update {
        val updated = it.selectedCuisinePreferences.toMutableSet()
        if (key in updated) updated.remove(key) else updated.add(key)
        it.copy(selectedCuisinePreferences = updated)
    }

    fun nextStep() {
        if (_state.value.currentStep < _state.value.totalSteps) {
            _state.update { it.copy(currentStep = it.currentStep + 1) }
        }
    }

    fun prevStep() {
        if (_state.value.currentStep > 1) {
            _state.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    fun completeOnboarding() {
        val s = _state.value
        _state.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            userPreferencesDataStore.updateProfile(
                name = s.name.trim(),
                age = s.age.toIntOrNull() ?: 0,
                weightKg = s.weightKg.toDoubleOrNull() ?: 0.0,
                targetWeightKg = s.targetWeightKg.toDoubleOrNull() ?: 0.0,
                heightCm = s.heightCm.toDoubleOrNull() ?: 0.0,
                activityLevel = s.activityLevel.name,
                workoutDaysPerWeek = s.workoutDaysPerWeek,
                hasShoulderRestriction = s.hasShoulderRestriction,
                dietaryRestrictions = s.selectedDietaryRestrictions.toList(),
                cuisinePreferences = s.selectedCuisinePreferences.toList()
            )
            appSettingsDataStore.setOnboardingComplete(true)
        }
    }
}
