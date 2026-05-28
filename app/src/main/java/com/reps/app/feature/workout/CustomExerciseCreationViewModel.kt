package com.reps.app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.ai.AIRepository
import com.reps.app.ai.EstimatedExercise
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CustomExerciseFormState(
    val name: String = "",
    val description: String = "",
    val muscleGroups: String = "",
    val equipment: String = "",
    val isShoulderSafe: Boolean = true,
    val restrictedMovements: String = "",
    val isEstimating: Boolean = false,
    val isSubmitting: Boolean = false,
    val nameError: Boolean = false,
    val estimationError: String? = null,
    val savedExerciseId: Long? = null
)

@HiltViewModel
class CustomExerciseCreationViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val aiRepository: AIRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(CustomExerciseFormState())
    val state: StateFlow<CustomExerciseFormState> = _state.asStateFlow()

    fun onNameChange(v: String) = _state.update { it.copy(name = v, nameError = false, estimationError = null) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }
    fun onMuscleGroupsChange(v: String) = _state.update { it.copy(muscleGroups = v) }
    fun onEquipmentChange(v: String) = _state.update { it.copy(equipment = v) }
    fun onShoulderSafeToggle(v: Boolean) = _state.update { it.copy(isShoulderSafe = v) }
    fun onRestrictedMovementsChange(v: String) = _state.update { it.copy(restrictedMovements = v) }

    fun estimateDetails() {
        val name = _state.value.name.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isEstimating = true, estimationError = null) }
            val result = withContext(ioDispatcher) { aiRepository.estimateExerciseDetails(name) }
            result.fold(
                onSuccess = { est ->
                    _state.update { s ->
                        s.copy(
                            isEstimating = false,
                            description = if (s.description.isBlank()) est.description else s.description,
                            muscleGroups = if (est.muscleGroups.isNotEmpty()) est.muscleGroups.joinToString(", ") else s.muscleGroups,
                            equipment = if (est.equipment.isNotEmpty()) est.equipment.joinToString(", ") else s.equipment,
                            isShoulderSafe = est.isShoulderSafe,
                            restrictedMovements = est.restrictedMovements.joinToString(", ")
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(isEstimating = false, estimationError = "Estimation failed. Fill details manually.") }
                }
            )
        }
    }

    fun saveExercise() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            try {
                val details = EstimatedExercise(
                    muscleGroups = s.muscleGroups.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    equipment = s.equipment.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    isShoulderSafe = s.isShoulderSafe,
                    restrictedMovements = s.restrictedMovements.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    description = s.description.trim()
                )
                val id = withContext(ioDispatcher) {
                    workoutRepository.createCustomExercise(s.name.trim(), details)
                }
                _state.update { it.copy(isSubmitting = false, savedExerciseId = id) }
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false, estimationError = "Failed to save exercise.") }
            }
        }
    }
}
