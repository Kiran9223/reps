package com.reps.app.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.WorkoutSession
import com.reps.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiveWorkoutUiState(
    val session: WorkoutSession? = null,
    val restSecondsRemaining: Int = 0,
    val isResting: Boolean = false,
    val isFinishing: Boolean = false
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val workoutLogId: Long = savedStateHandle["workoutLogId"] ?: -1L
    private val _restSecondsRemaining = MutableStateFlow(0)
    private val _isResting = MutableStateFlow(false)
    private val _isFinishing = MutableStateFlow(false)
    private var restJob: Job? = null

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        workoutRepository.getSession(workoutLogId),
        _restSecondsRemaining,
        _isResting,
        _isFinishing
    ) { session, restSecs, isResting, isFinishing ->
        ActiveWorkoutUiState(
            session = session,
            restSecondsRemaining = restSecs,
            isResting = isResting,
            isFinishing = isFinishing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

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
