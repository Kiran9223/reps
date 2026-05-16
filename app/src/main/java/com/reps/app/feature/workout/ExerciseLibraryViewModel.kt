package com.reps.app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.Exercise
import com.reps.app.core.domain.model.ExerciseFilter
import com.reps.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ExerciseLibraryUiState(
    val exercises: List<Exercise> = emptyList(),
    val query: String = "",
    val shoulderSafeOnly: Boolean = false,
    val selectedMuscleGroup: String? = null
)

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _shoulderSafeOnly = MutableStateFlow(false)
    private val _selectedMuscleGroup = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<ExerciseLibraryUiState> = combine(
        _query.debounce(200),
        _shoulderSafeOnly,
        _selectedMuscleGroup
    ) { query, shoulderSafe, muscle ->
        ExerciseFilter(query = query, shoulderSafeOnly = shoulderSafe, muscleGroup = muscle)
    }.flatMapLatest { filter ->
        workoutRepository.getExercises(filter).combine(
            combine(_query, _shoulderSafeOnly, _selectedMuscleGroup) { q, s, m -> Triple(q, s, m) }
        ) { exercises, (q, s, m) ->
            ExerciseLibraryUiState(
                exercises = exercises,
                query = q,
                shoulderSafeOnly = s,
                selectedMuscleGroup = m
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryUiState())

    fun setQuery(query: String) { _query.value = query }
    fun toggleShoulderSafe() { _shoulderSafeOnly.value = !_shoulderSafeOnly.value }
    fun selectMuscleGroup(group: String?) { _selectedMuscleGroup.value = group }
}
