package com.reps.app.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.ai.AIRepository
import com.reps.app.ai.ExerciseAlternative
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import com.reps.app.core.domain.model.WorkoutSession
import com.reps.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoulderWarning(
    val exerciseName: String,
    val muscleGroup: String,
    val alternatives: List<ExerciseAlternative> = emptyList(),
    val isFetching: Boolean = false,
    val alternativesFailed: Boolean = false
)

data class ActiveWorkoutUiState(
    val session: WorkoutSession? = null,
    val restSecondsRemaining: Int = 0,
    val isResting: Boolean = false,
    val isFinishing: Boolean = false,
    val shoulderWarning: ShoulderWarning? = null,
    val personalRecords: Map<Long, Double> = emptyMap()
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val aiRepository: AIRepository,
    private val userPrefs: UserPreferencesDataStore
) : ViewModel() {

    private val workoutLogId: Long = savedStateHandle["workoutLogId"] ?: -1L
    private val _restSecondsRemaining = MutableStateFlow(0)
    private val _isResting = MutableStateFlow(false)
    private val _isFinishing = MutableStateFlow(false)
    private val _shoulderWarning = MutableStateFlow<ShoulderWarning?>(null)
    private val _personalRecords = MutableStateFlow<Map<Long, Double>>(emptyMap())
    private var restJob: Job? = null

    init {
        checkShoulderSafety()
        loadPersonalRecords()
    }

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        combine(workoutRepository.getSession(workoutLogId), _restSecondsRemaining, _isResting) { s, r, i -> Triple(s, r, i) },
        combine(_isFinishing, _shoulderWarning, _personalRecords) { f, w, pr -> Triple(f, w, pr) }
    ) { (session, restSecs, isResting), (isFinishing, warning, prs) ->
        ActiveWorkoutUiState(
            session = session,
            restSecondsRemaining = restSecs,
            isResting = isResting,
            isFinishing = isFinishing,
            shoulderWarning = warning,
            personalRecords = prs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    private fun loadPersonalRecords() {
        viewModelScope.launch {
            val session = workoutRepository.getSession(workoutLogId).first { it != null } ?: return@launch
            val records = session.exercises.mapNotNull { sessionExercise ->
                val pr = workoutRepository.getPersonalRecord(sessionExercise.exercise.id)
                pr?.let { sessionExercise.exercise.id to it }
            }.toMap()
            _personalRecords.value = records
        }
    }

    private fun checkShoulderSafety() {
        viewModelScope.launch {
            val hasRestriction = userPrefs.hasShoulderRestriction.first()
            if (!hasRestriction) return@launch
            val session = workoutRepository.getSession(workoutLogId).first { it != null }
            val unsafeExercise = session?.exercises?.firstOrNull { !it.exercise.isShoulderSafe }
                ?: return@launch
            val exerciseName = unsafeExercise.exercise.name
            val muscleGroup = unsafeExercise.exercise.muscleGroups.firstOrNull() ?: "General"
            _shoulderWarning.value = ShoulderWarning(exerciseName, muscleGroup, isFetching = true)
            fetchShoulderAlternatives(exerciseName, muscleGroup)
        }
    }

    fun retryShoulderAlternatives() {
        val warning = _shoulderWarning.value ?: return
        viewModelScope.launch {
            fetchShoulderAlternatives(warning.exerciseName, warning.muscleGroup)
        }
    }

    private suspend fun fetchShoulderAlternatives(exerciseName: String, muscleGroup: String) {
        _shoulderWarning.value = ShoulderWarning(
            exerciseName,
            muscleGroup,
            isFetching = true,
            alternativesFailed = false
        )
        aiRepository.getShoulderSafeAlternatives(exerciseName, muscleGroup)
            .onSuccess { alts ->
                _shoulderWarning.value = ShoulderWarning(
                    exerciseName,
                    muscleGroup,
                    alts,
                    isFetching = false,
                    alternativesFailed = false
                )
            }
            .onFailure {
                _shoulderWarning.value = ShoulderWarning(
                    exerciseName,
                    muscleGroup,
                    isFetching = false,
                    alternativesFailed = true
                )
            }
    }

    fun dismissShoulderWarning() {
        _shoulderWarning.value = null
    }

    fun completeSet(setId: Long, reps: Int?, weightKg: Double?, restSeconds: Int = 90) {
        viewModelScope.launch {
            workoutRepository.completeSet(setId, reps, weightKg)
            startRestTimer(restSeconds)
        }
    }

    private fun startRestTimer(seconds: Int) {
        restJob?.cancel()
        _isResting.value = true
        _restSecondsRemaining.value = seconds
        restJob = viewModelScope.launch {
            repeat(seconds) {
                delay(1_000)
                _restSecondsRemaining.value -= 1
            }
            _isResting.value = false
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _isResting.value = false
        _restSecondsRemaining.value = 0
    }

    fun finishWorkout(onDone: () -> Unit) {
        viewModelScope.launch {
            _isFinishing.value = true
            workoutRepository.completeWorkout(workoutLogId)
            _isFinishing.value = false
            onDone()
        }
    }
}
