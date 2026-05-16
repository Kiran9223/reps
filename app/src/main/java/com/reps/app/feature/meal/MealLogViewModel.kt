package com.reps.app.feature.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.repository.MealLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealLogViewModel @Inject constructor(
    private val mealLogRepository: MealLogRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val dayLog: StateFlow<DayLog> = _selectedDate
        .flatMapLatest { date -> mealLogRepository.getDayLog(date.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DayLog(LocalDate.now().toString()))

    private val _pendingDeleteId = MutableStateFlow<Long?>(null)
    val pendingDeleteId: StateFlow<Long?> = _pendingDeleteId.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun onRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(400) // data is already live via Room; brief spinner for feedback
            _isRefreshing.value = false
        }
    }

    fun onPreviousDay() { _selectedDate.value = _selectedDate.value.minusDays(1) }

    fun onNextDay() {
        if (_selectedDate.value < LocalDate.now()) {
            _selectedDate.value = _selectedDate.value.plusDays(1)
        }
    }

    fun onSwipeToDelete(entryId: Long) {
        _pendingDeleteId.value = entryId
    }

    fun undoDelete() {
        _pendingDeleteId.value = null
    }

    fun commitDelete() {
        val id = _pendingDeleteId.value ?: return
        _pendingDeleteId.value = null
        viewModelScope.launch { mealLogRepository.removeFoodFromLog(id) }
    }

    fun getSelectedDateStr(): String = _selectedDate.value.toString()
}
