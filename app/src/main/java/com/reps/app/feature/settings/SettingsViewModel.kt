package com.reps.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.ai.AIPrivacyStatus
import com.reps.app.ai.CloudAssistGate
import com.reps.app.ai.OnDeviceModelConfig
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import com.reps.app.core.domain.ProfileInput
import com.reps.app.core.domain.ProfileValidationError
import com.reps.app.core.domain.toPersistedValues
import com.reps.app.core.domain.validate
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

private data class SettingsSnapshot(
    val name: String,
    val age: String,
    val weightKg: String,
    val targetWeightKg: String,
    val heightCm: String,
    val activityLevel: ActivityLevel,
    val workoutDaysPerWeek: Int,
    val hasShoulderRestriction: Boolean,
    val selectedDietaryRestrictions: Set<String>,
    val selectedCuisinePreferences: Set<String>
) {
    companion object {
        fun from(state: SettingsUiState) = SettingsSnapshot(
            name = state.name,
            age = state.age,
            weightKg = state.weightKg,
            targetWeightKg = state.targetWeightKg,
            heightCm = state.heightCm,
            activityLevel = state.activityLevel,
            workoutDaysPerWeek = state.workoutDaysPerWeek,
            hasShoulderRestriction = state.hasShoulderRestriction,
            selectedDietaryRestrictions = state.selectedDietaryRestrictions,
            selectedCuisinePreferences = state.selectedCuisinePreferences
        )
    }
}

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
    val savedSuccess: Boolean = false,
    val saveFailed: Boolean = false,
    val validationError: ProfileValidationError? = null,
    val showDiscardDialog: Boolean = false,
    val onDeviceLlmActive: Boolean = false,
    val cloudAssistAvailable: Boolean = false,
    val cloudAssistUserEnabled: Boolean = true,
    val cloudAssistActive: Boolean = false,
    val onDeviceModelInstalled: Boolean = false,
    val proteinRemindersEnabled: Boolean = true,
    val isDirty: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferencesDataStore,
    private val appSettings: AppSettingsDataStore,
    private val cloudAssistGate: CloudAssistGate,
    aiPrivacyStatus: AIPrivacyStatus
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            onDeviceLlmActive = aiPrivacyStatus.onDeviceLlmActive,
            cloudAssistAvailable = aiPrivacyStatus.cloudAssistAvailable,
            onDeviceModelInstalled = OnDeviceModelConfig.isModelInstalled()
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private var savedSnapshot: SettingsSnapshot? = null

    init {
        viewModelScope.launch {
            val weight = userPrefs.weightKg.first()
            val target = userPrefs.targetWeightKg.first()
            val height = userPrefs.heightCm.first()
            val age = userPrefs.age.first()
            val activity = runCatching {
                ActivityLevel.valueOf(userPrefs.activityLevel.first())
            }.getOrDefault(ActivityLevel.MODERATE)

            val loaded = SettingsUiState(
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
                isLoaded = true,
                onDeviceLlmActive = aiPrivacyStatus.onDeviceLlmActive,
                cloudAssistAvailable = aiPrivacyStatus.cloudAssistAvailable,
                cloudAssistUserEnabled = appSettings.cloudAssistUserEnabled.first(),
                cloudAssistActive = cloudAssistGate.isCloudActiveNow(),
                onDeviceModelInstalled = OnDeviceModelConfig.isModelInstalled(),
                proteinRemindersEnabled = appSettings.proteinRemindersEnabled.first()
            )
            savedSnapshot = SettingsSnapshot.from(loaded)
            _state.value = loaded.copy(isDirty = false)
        }
        viewModelScope.launch {
            cloudAssistGate.isCloudActive.collect { active ->
                _state.update { it.copy(cloudAssistActive = active) }
            }
        }
    }

    fun refreshModelStatus() {
        _state.update { it.copy(onDeviceModelInstalled = OnDeviceModelConfig.isModelInstalled()) }
    }

    fun setCloudAssistUserEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setCloudAssistUserEnabled(enabled)
            _state.update { it.copy(cloudAssistUserEnabled = enabled) }
        }
    }

    fun setProteinRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setProteinRemindersEnabled(enabled)
            _state.update { it.copy(proteinRemindersEnabled = enabled) }
        }
    }

    private fun syncDirtyFlag() {
        val dirty = savedSnapshot != null && SettingsSnapshot.from(_state.value) != savedSnapshot
        if (_state.value.isDirty != dirty) {
            _state.update { it.copy(isDirty = dirty) }
        }
    }

    private fun SettingsUiState.clearingFeedback() = copy(
        savedSuccess = false,
        saveFailed = false,
        validationError = null
    )

    fun onNameChange(v: String) = _state.update { it.copy(name = v).clearingFeedback() }.also { syncDirtyFlag() }
    fun onAgeChange(v: String) = _state.update { it.copy(age = v).clearingFeedback() }.also { syncDirtyFlag() }
    fun onWeightChange(v: String) = _state.update { it.copy(weightKg = v).clearingFeedback() }.also { syncDirtyFlag() }
    fun onTargetWeightChange(v: String) = _state.update { it.copy(targetWeightKg = v).clearingFeedback() }.also { syncDirtyFlag() }
    fun onHeightChange(v: String) = _state.update { it.copy(heightCm = v).clearingFeedback() }.also { syncDirtyFlag() }
    fun onActivityLevelChange(v: ActivityLevel) = _state.update { it.copy(activityLevel = v).clearingFeedback() }.also { syncDirtyFlag() }
    fun onWorkoutDaysChange(v: Int) = _state.update { it.copy(workoutDaysPerWeek = v).clearingFeedback() }.also { syncDirtyFlag() }
    fun onShoulderChange(v: Boolean) = _state.update { it.copy(hasShoulderRestriction = v).clearingFeedback() }.also { syncDirtyFlag() }

    fun onDietaryToggle(key: String) {
        _state.update {
            val updated = if (key in it.selectedDietaryRestrictions)
                it.selectedDietaryRestrictions - key else it.selectedDietaryRestrictions + key
            it.copy(selectedDietaryRestrictions = updated, savedSuccess = false, saveFailed = false, validationError = null)
        }
        syncDirtyFlag()
    }

    fun onCuisineToggle(key: String) {
        _state.update {
            val updated = if (key in it.selectedCuisinePreferences)
                it.selectedCuisinePreferences - key else it.selectedCuisinePreferences + key
            it.copy(selectedCuisinePreferences = updated, savedSuccess = false, saveFailed = false, validationError = null)
        }
        syncDirtyFlag()
    }

    /** @return true if caller should navigate back immediately */
    fun tryNavigateBack(): Boolean {
        if (!_state.value.isDirty) return true
        _state.update { it.copy(showDiscardDialog = true) }
        return false
    }

    fun dismissDiscardDialog() = _state.update { it.copy(showDiscardDialog = false) }

    fun confirmDiscard() {
        val snap = savedSnapshot ?: return
        _state.update {
            it.copy(
                name = snap.name,
                age = snap.age,
                weightKg = snap.weightKg,
                targetWeightKg = snap.targetWeightKg,
                heightCm = snap.heightCm,
                activityLevel = snap.activityLevel,
                workoutDaysPerWeek = snap.workoutDaysPerWeek,
                hasShoulderRestriction = snap.hasShoulderRestriction,
                selectedDietaryRestrictions = snap.selectedDietaryRestrictions,
                selectedCuisinePreferences = snap.selectedCuisinePreferences,
                showDiscardDialog = false,
                validationError = null,
                saveFailed = false,
                savedSuccess = false,
                isDirty = false
            )
        }
    }

    fun save() {
        val s = _state.value
        val input = ProfileInput(s.name, s.age, s.weightKg, s.targetWeightKg, s.heightCm)
        val validationError = input.validate()
        if (validationError != null) {
            _state.update { it.copy(validationError = validationError, saveFailed = false, savedSuccess = false) }
            return
        }
        val persisted = input.toPersistedValues() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, savedSuccess = false, saveFailed = false, validationError = null) }
            try {
                userPrefs.updateProfile(
                    name = persisted.name,
                    age = persisted.age,
                    weightKg = persisted.weightKg,
                    targetWeightKg = persisted.targetWeightKg,
                    heightCm = persisted.heightCm,
                    activityLevel = s.activityLevel.name,
                    workoutDaysPerWeek = s.workoutDaysPerWeek,
                    hasShoulderRestriction = s.hasShoulderRestriction,
                    dietaryRestrictions = s.selectedDietaryRestrictions.toList(),
                    cuisinePreferences = s.selectedCuisinePreferences.toList()
                )
                savedSnapshot = SettingsSnapshot.from(_state.value)
                _state.update {
                    it.copy(
                        isSaving = false,
                        savedSuccess = true,
                        saveFailed = false,
                        isDirty = false
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, saveFailed = true, savedSuccess = false) }
            }
        }
    }
}
