package com.reps.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import com.reps.app.feature.onboarding.ActivityLevel
import com.reps.app.feature.onboarding.OnboardingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
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
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferencesDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val weight = userPrefs.weightKg.first()
            val target = userPrefs.targetWeightKg.first()
            val height = userPrefs.heightCm.first()
            val age = userPrefs.age.first()
            val activity = runCatching {
                ActivityLevel.valueOf(userPrefs.activityLevel.first())
            }.getOrDefault(ActivityLevel.MODERATE)

            _state.update {
                it.copy(
                    name = userPrefs.name.first(),
                    age = if (age > 0) age.toString() else "",
                    weightKg = if (weight > 0) weight.toString() else "",
                    targetWeightKg = if (target > 0) target.toString() else "",
                    heightCm = if (height > 0) height.toString() else "",
                    activityLevel = activity,
                    workoutDaysPerWeek = userPrefs.workoutDaysPerWeek.first(),
                    hasShoulderRestriction = userPrefs.hasShoulderRestriction.first(),
                    selectedDietaryRestrictions = userPrefs.dietaryRestrictions.first().toSet(),
                    selectedCuisinePreferences = userPrefs.cuisinePreferences.first().toSet(),
                    isLoaded = true
                )
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v, savedSuccess = false) }
    fun onAgeChange(v: String) = _state.update { it.copy(age = v, savedSuccess = false) }
    fun onWeightChange(v: String) = _state.update { it.copy(weightKg = v, savedSuccess = false) }
    fun onTargetWeightChange(v: String) = _state.update { it.copy(targetWeightKg = v, savedSuccess = false) }
    fun onHeightChange(v: String) = _state.update { it.copy(heightCm = v, savedSuccess = false) }
    fun onActivityLevelChange(v: ActivityLevel) = _state.update { it.copy(activityLevel = v, savedSuccess = false) }
    fun onWorkoutDaysChange(v: Int) = _state.update { it.copy(workoutDaysPerWeek = v, savedSuccess = false) }
    fun onShoulderChange(v: Boolean) = _state.update { it.copy(hasShoulderRestriction = v, savedSuccess = false) }

    fun onDietaryToggle(key: String) = _state.update {
        val updated = if (key in it.selectedDietaryRestrictions)
            it.selectedDietaryRestrictions - key else it.selectedDietaryRestrictions + key
        it.copy(selectedDietaryRestrictions = updated, savedSuccess = false)
    }

    fun onCuisineToggle(key: String) = _state.update {
        val updated = if (key in it.selectedCuisinePreferences)
            it.selectedCuisinePreferences - key else it.selectedCuisinePreferences + key
        it.copy(selectedCuisinePreferences = updated, savedSuccess = false)
    }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, savedSuccess = false) }
            userPrefs.updateProfile(
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
            _state.update { it.copy(isSaving = false, savedSuccess = true) }
        }
    }
}
