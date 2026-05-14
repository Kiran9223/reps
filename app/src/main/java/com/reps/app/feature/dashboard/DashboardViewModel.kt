package com.reps.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import com.reps.app.core.domain.MacroCalculator
import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.DayMacros
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.repository.MealLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class DashboardTab { TODAY, PROGRESS }

data class WeekStats(
    val proteinHitDays: Int = 0,
    val avgCalories: Int = 0,
    val currentStreak: Int = 0
)

data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val dayLog: DayLog = DayLog(date = LocalDate.now().toString()),
    val macros: DayMacros = DayMacros(),
    val waterMl: Int = 0,
    val waterTarget: Int = 2500,
    val selectedTab: DashboardTab = DashboardTab.TODAY,
    val weekHistory: List<DayLog> = emptyList(),
    val weekStats: WeekStats = WeekStats(),
    val activeSheet: MealSlot? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val mealLogRepository: MealLogRepository,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val userPrefs: UserPreferencesDataStore
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _selectedTab = MutableStateFlow(DashboardTab.TODAY)
    private val _activeSheet = MutableStateFlow<MealSlot?>(null)

    private val macroTargets: StateFlow<MacroTargets> = combine(
        userPrefs.weightKg, userPrefs.heightCm, userPrefs.age, userPrefs.activityLevel
    ) { weight, height, age, activity ->
        if (weight > 0 && height > 0 && age > 0)
            MacroCalculator.compute(weight, height, age, activity)
        else MacroTargets.DEFAULT
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), MacroTargets.DEFAULT)

    private val dayLog: StateFlow<DayLog> = _selectedDate
        .flatMapLatest { date -> mealLogRepository.getDayLog(date.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DayLog(LocalDate.now().toString()))

    private val macros: StateFlow<DayMacros> = combine(_selectedDate, macroTargets) { date, targets ->
        date to targets
    }.flatMapLatest { (date, targets) ->
        mealLogRepository.getDayMacros(date.toString(), targets)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DayMacros())

    private val waterMl: StateFlow<Int> = _selectedDate
        .flatMapLatest { date -> appSettingsDataStore.getWaterMl(date.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 0)

    private val weekHistory: StateFlow<List<DayLog>> = mealLogRepository.getLogHistory(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private data class DayBundle(val log: DayLog, val macros: DayMacros, val water: Int, val targets: MacroTargets)

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(dayLog, macros, waterMl, macroTargets) { log, m, water, targets ->
            DayBundle(log, m, water, targets)
        },
        combine(_selectedDate, _selectedTab, _activeSheet) { date, tab, sheet ->
            Triple(date, tab, sheet)
        },
        weekHistory
    ) { bundle, (date, tab, sheet), history ->
        DashboardUiState(
            selectedDate = date,
            dayLog = bundle.log,
            macros = bundle.macros,
            waterMl = bundle.water,
            waterTarget = bundle.targets.waterMl,
            selectedTab = tab,
            weekHistory = history,
            weekStats = computeWeekStats(history, bundle.targets),
            activeSheet = sheet
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DashboardUiState())

    fun onPreviousDay() { _selectedDate.value = _selectedDate.value.minusDays(1) }

    fun onNextDay() {
        if (_selectedDate.value < LocalDate.now()) {
            _selectedDate.value = _selectedDate.value.plusDays(1)
        }
    }

    fun onTabChange(tab: DashboardTab) { _selectedTab.value = tab }
    fun openSlotSheet(slot: MealSlot) { _activeSheet.value = slot }
    fun closeSlotSheet() { _activeSheet.value = null }
    fun getSelectedDateStr(): String = _selectedDate.value.toString()
    fun getSelectedSlotName(): String = _activeSheet.value?.name ?: ""

    fun addWater() {
        viewModelScope.launch {
            appSettingsDataStore.addWater(_selectedDate.value.toString(), 250)
        }
    }

    fun removeLogEntry(entryId: Long) {
        viewModelScope.launch { mealLogRepository.removeFoodFromLog(entryId) }
    }

    private fun computeWeekStats(history: List<DayLog>, targets: MacroTargets): WeekStats {
        if (history.isEmpty()) return WeekStats()
        val proteinHitDays = history.count { it.totalProtein >= targets.proteinG }
        val avgCalories = history.map { it.totalCalories }.average().toInt()
        val sorted = history.sortedByDescending { it.date }
        var streak = 0
        for (log in sorted) {
            if (log.totalProtein >= targets.proteinG) streak++ else break
        }
        return WeekStats(proteinHitDays, avgCalories, streak)
    }
}
