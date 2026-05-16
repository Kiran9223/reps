package com.reps.app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.domain.model.ExerciseFilter
import com.reps.app.core.domain.model.WorkoutSummary
import com.reps.app.core.domain.model.WorkoutTemplate
import com.reps.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutUiState(
    val templates: List<WorkoutTemplate> = emptyList(),
    val recentWorkouts: List<WorkoutSummary> = emptyList(),
    val isShoulderSafeOnly: Boolean = false,
    val isStarting: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val appSettingsDataStore: AppSettingsDataStore
) : ViewModel() {

    private val _isStarting = MutableStateFlow(false)

    val uiState: StateFlow<WorkoutUiState> = combine(
        workoutRepository.getTemplates(),
        workoutRepository.getWorkoutHistory(limit = 5),
        appSettingsDataStore.isShoulderSafeOnly,
        _isStarting
    ) { templates, recent, shoulderSafe, starting ->
        WorkoutUiState(
            templates = templates,
            recentWorkouts = recent,
            isShoulderSafeOnly = shoulderSafe,
            isStarting = starting
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    fun toggleShoulderSafe() {
        viewModelScope.launch {
            appSettingsDataStore.setShoulderSafeOnly(!uiState.value.isShoulderSafeOnly)
        }
    }

    suspend fun startWorkout(templateId: Long?): Long {
        _isStarting.value = true
        return try {
            workoutRepository.startWorkout(templateId)
        } finally {
            _isStarting.value = false
        }
    }

    fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteTemplate(templateId)
        }
    }
}
