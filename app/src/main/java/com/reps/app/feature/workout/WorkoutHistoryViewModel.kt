package com.reps.app.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.WorkoutSession
import com.reps.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    workoutRepository: WorkoutRepository
) : ViewModel() {

    private val workoutLogId: Long = savedStateHandle["workoutLogId"] ?: -1L

    val session: StateFlow<WorkoutSession?> = workoutRepository
        .getSession(workoutLogId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
