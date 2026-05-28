package com.reps.app.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.ai.AIRepository
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.model.ExerciseFilter
import com.reps.app.core.domain.model.TemplateExerciseDraft
import com.reps.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DraftExerciseItem(
    val exerciseId: Long,
    val exerciseName: String,
    val targetSets: Int = 3,
    val targetReps: String = "8-12",
    val targetWeightKg: Double? = null
)

data class CreateTemplateState(
    val templateId: Long? = null,
    val name: String = "",
    val description: String = "",
    val exercises: List<DraftExerciseItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEstimating: Boolean = false,
    val nameError: Boolean = false,
    val noExerciseError: Boolean = false,
    val estimationError: String? = null,
    val savedTemplateId: Long? = null
)

@HiltViewModel
class CreateWorkoutTemplateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val aiRepository: AIRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val editTemplateId: Long? =
        savedStateHandle.get<Long>("templateId")?.takeIf { it >= 0L }

    private val _state = MutableStateFlow(CreateTemplateState(templateId = editTemplateId))
    val state: StateFlow<CreateTemplateState> = _state.asStateFlow()

    init {
        if (editTemplateId != null) loadExistingTemplate(editTemplateId)
    }

    private fun loadExistingTemplate(templateId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val template = workoutRepository.getTemplates().first()
                .find { it.id == templateId }
            if (template == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            _state.value = _state.value.copy(
                name = template.name,
                description = template.description ?: "",
                exercises = template.exercises.sortedBy { it.sortOrder }.map { te ->
                    DraftExerciseItem(
                        exerciseId = te.exercise.id,
                        exerciseName = te.exercise.name,
                        targetSets = te.targetSets,
                        targetReps = te.targetReps,
                        targetWeightKg = te.targetWeightKg
                    )
                },
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

    fun addExercise(exerciseId: Long, exerciseName: String) {
        val s = _state.value
        if (s.exercises.any { it.exerciseId == exerciseId }) return
        _state.value = s.copy(
            exercises = s.exercises + DraftExerciseItem(exerciseId, exerciseName),
            noExerciseError = false
        )
    }

    fun removeExercise(exerciseId: Long) {
        _state.value = _state.value.copy(
            exercises = _state.value.exercises.filterNot { it.exerciseId == exerciseId }
        )
    }

    fun updateExerciseSets(exerciseId: Long, sets: Int) {
        _state.value = _state.value.copy(
            exercises = _state.value.exercises.map {
                if (it.exerciseId == exerciseId) it.copy(targetSets = sets.coerceIn(1, 20)) else it
            }
        )
    }

    fun updateExerciseReps(exerciseId: Long, reps: String) {
        _state.value = _state.value.copy(
            exercises = _state.value.exercises.map {
                if (it.exerciseId == exerciseId) it.copy(targetReps = reps) else it
            }
        )
    }

    fun updateExerciseWeight(exerciseId: Long, weightKg: Double?) {
        _state.value = _state.value.copy(
            exercises = _state.value.exercises.map {
                if (it.exerciseId == exerciseId) it.copy(targetWeightKg = weightKg) else it
            }
        )
    }

    fun estimateWorkout() {
        val name = _state.value.name.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isEstimating = true, estimationError = null)
            val exercises = withContext(ioDispatcher) {
                workoutRepository.getExercises(ExerciseFilter()).first()
            }
            val result = withContext(ioDispatcher) {
                aiRepository.estimateWorkoutTemplate(name, exercises)
            }
            result.fold(
                onSuccess = { suggestion ->
                    val exerciseMap = exercises.associateBy { it.id }
                    _state.value = _state.value.copy(
                        isEstimating = false,
                        description = if (_state.value.description.isBlank()) suggestion.description else _state.value.description,
                        exercises = suggestion.exercises.mapNotNull { se ->
                            val ex = exerciseMap[se.exerciseId] ?: return@mapNotNull null
                            DraftExerciseItem(
                                exerciseId = se.exerciseId,
                                exerciseName = ex.name,
                                targetSets = se.targetSets,
                                targetReps = se.targetReps,
                                targetWeightKg = se.targetWeightKg
                            )
                        },
                        noExerciseError = false
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        isEstimating = false,
                        estimationError = "Generation failed. Add exercises manually."
                    )
                }
            )
        }
    }

    fun saveTemplate() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(nameError = true)
            return
        }
        if (s.exercises.isEmpty()) {
            _state.value = s.copy(noExerciseError = true)
            return
        }
        val drafts = s.exercises.map { item ->
            TemplateExerciseDraft(
                exerciseId = item.exerciseId,
                targetSets = item.targetSets,
                targetReps = item.targetReps,
                targetWeightKg = item.targetWeightKg
            )
        }
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true)
            val savedId = if (s.templateId == null) {
                workoutRepository.createTemplate(
                    name = s.name.trim(),
                    description = s.description.trim().takeIf { it.isNotBlank() },
                    exercises = drafts
                )
            } else {
                workoutRepository.updateTemplate(
                    templateId = s.templateId,
                    name = s.name.trim(),
                    description = s.description.trim().takeIf { it.isNotBlank() },
                    exercises = drafts
                )
                s.templateId
            }
            _state.value = _state.value.copy(isSaving = false, savedTemplateId = savedId)
        }
    }
}
