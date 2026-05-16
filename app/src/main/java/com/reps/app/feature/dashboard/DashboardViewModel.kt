package com.reps.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.ai.AIRepository
import com.reps.app.ai.AiMealSuggestion
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import com.reps.app.core.data.repository.ProgressRepositoryImpl
import com.reps.app.core.domain.MacroCalculator
import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.DayMacros
import com.reps.app.core.domain.model.GoalProgress
import com.reps.app.core.domain.model.LoggedFood
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.repository.FoodRepository
import com.reps.app.core.domain.repository.MealLogRepository
import com.reps.app.core.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val activeSheet: MealSlot? = null,
    val goalProgress: GoalProgress? = null,
    val dailyInsight: String? = null,
    val isFetchingInsight: Boolean = false,
    val mealSuggestion: AiMealSuggestion? = null,
    val isFetchingSuggestion: Boolean = false,
    val pendingDeleteEntryId: Long? = null,
    val editingEntry: LoggedFood? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val mealLogRepository: MealLogRepository,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val userPrefs: UserPreferencesDataStore,
    private val progressRepository: ProgressRepository,
    private val aiRepository: AIRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _selectedTab = MutableStateFlow(DashboardTab.TODAY)
    private val _activeSheet = MutableStateFlow<MealSlot?>(null)
    private val _mealSuggestion = MutableStateFlow<AiMealSuggestion?>(null)
    private val _isFetchingSuggestion = MutableStateFlow(false)
    private val _isFetchingInsight = MutableStateFlow(false)
    private val _pendingDeleteEntryId = MutableStateFlow<Long?>(null)
    private val _editingEntry = MutableStateFlow<LoggedFood?>(null)

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

    private val goalProgress: StateFlow<GoalProgress?> = combine(
        userPrefs.weightKg,
        userPrefs.targetWeightKg,
        progressRepository.getWeightHistory()
    ) { startWeight, targetWeight, history ->
        if (startWeight <= 0 || targetWeight <= 0) return@combine null
        val currentWeight = history.firstOrNull()?.weightKg ?: startWeight
        val isLosingWeight = targetWeight < startWeight
        val totalDelta = if (isLosingWeight) startWeight - targetWeight else targetWeight - startWeight
        val achieved = if (isLosingWeight) (startWeight - currentWeight).coerceAtLeast(0.0)
                        else (currentWeight - startWeight).coerceAtLeast(0.0)
        val fraction = if (totalDelta > 0) (achieved / totalDelta).toFloat().coerceIn(0f, 1f) else 1f
        val remaining = if (isLosingWeight) (currentWeight - targetWeight).coerceAtLeast(0.0)
                        else (targetWeight - currentWeight).coerceAtLeast(0.0)
        GoalProgress(
            startWeightKg = startWeight,
            currentWeightKg = currentWeight,
            targetWeightKg = targetWeight,
            kgRemaining = remaining,
            progressFraction = fraction,
            estimatedWeeksToGoal = ProgressRepositoryImpl.projectWeeksToGoal(history, targetWeight)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    private val dailyInsight: StateFlow<String?> = appSettingsDataStore.dailyInsight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    private data class DayBundle(val log: DayLog, val macros: DayMacros, val water: Int, val targets: MacroTargets)
    private data class AiBundle(
        val isFetchingInsight: Boolean,
        val insight: String?,
        val suggestion: AiMealSuggestion?,
        val isFetchingSuggestion: Boolean
    )

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(dayLog, macros, waterMl, macroTargets) { log, m, water, targets ->
            DayBundle(log, m, water, targets)
        },
        combine(_selectedDate, _selectedTab, _activeSheet) { date, tab, sheet ->
            Triple(date, tab, sheet)
        },
        combine(weekHistory, goalProgress) { history, gp -> history to gp },
        combine(_isFetchingInsight, dailyInsight, _mealSuggestion, _isFetchingSuggestion) { f, i, s, fs ->
            AiBundle(f, i, s, fs)
        },
        combine(_pendingDeleteEntryId, _editingEntry) { pending, editing -> pending to editing }
    ) { bundle, (date, tab, sheet), (history, gp), ai, (pending, editing) ->
        DashboardUiState(
            selectedDate = date,
            dayLog = bundle.log,
            macros = bundle.macros,
            waterMl = bundle.water,
            waterTarget = bundle.targets.waterMl,
            selectedTab = tab,
            weekHistory = history,
            weekStats = computeWeekStats(history, bundle.targets),
            activeSheet = sheet,
            goalProgress = gp,
            dailyInsight = ai.insight,
            isFetchingInsight = ai.isFetchingInsight,
            mealSuggestion = ai.suggestion,
            isFetchingSuggestion = ai.isFetchingSuggestion,
            pendingDeleteEntryId = pending,
            editingEntry = editing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DashboardUiState())

    fun onPreviousDay() { _selectedDate.value = _selectedDate.value.minusDays(1) }

    fun onNextDay() {
        if (_selectedDate.value < LocalDate.now()) {
            _selectedDate.value = _selectedDate.value.plusDays(1)
        }
    }

    fun onTabChange(tab: DashboardTab) { _selectedTab.value = tab }

    fun openSlotSheet(slot: MealSlot) {
        _mealSuggestion.value = null
        _activeSheet.value = slot
    }

    fun closeSlotSheet() {
        _activeSheet.value = null
        _mealSuggestion.value = null
    }

    fun getSelectedDateStr(): String = _selectedDate.value.toString()
    fun getSelectedSlotName(): String = _activeSheet.value?.name ?: ""

    fun addWater(ml: Int) {
        viewModelScope.launch {
            appSettingsDataStore.addWater(_selectedDate.value.toString(), ml)
        }
    }

    fun openEditEntry(entry: LoggedFood) { _editingEntry.value = entry }
    fun closeEditEntry() { _editingEntry.value = null }

    fun saveEditedServings(entryId: Long, servings: Double) {
        viewModelScope.launch {
            mealLogRepository.updateServings(entryId, servings)
            closeEditEntry()
        }
    }

    fun onSwipeToDelete(entryId: Long) {
        _pendingDeleteEntryId.value = entryId
        _activeSheet.value = null
        _mealSuggestion.value = null
    }

    fun undoDelete() {
        _pendingDeleteEntryId.value = null
    }

    fun commitDelete() {
        val id = _pendingDeleteEntryId.value ?: return
        _pendingDeleteEntryId.value = null
        viewModelScope.launch { mealLogRepository.removeFoodFromLog(id) }
    }

    fun requestInsight() {
        if (_isFetchingInsight.value) return
        viewModelScope.launch {
            _isFetchingInsight.value = true
            val log = dayLog.value
            val targets = macroTargets.value
            val weightKg = userPrefs.weightKg.first()
            val targetWeight = userPrefs.targetWeightKg.first()
            val waterMlNow = appSettingsDataStore.getWaterMl(_selectedDate.value.toString()).first()
            aiRepository.getDailyInsight(log, targets, weightKg, targetWeight, false, waterMlNow)
                .onSuccess { appSettingsDataStore.saveDailyInsight(it) }
            _isFetchingInsight.value = false
        }
    }

    fun addSuggestedMealToLog() {
        val suggestion = _mealSuggestion.value ?: return
        val slot = _activeSheet.value ?: return
        viewModelScope.launch {
            foodRepository.searchFoods(suggestion.foodName).first()
                .firstOrNull()
                ?.let { food ->
                    mealLogRepository.addFoodToLog(
                        _selectedDate.value.toString(),
                        slot,
                        food.id,
                        suggestion.servings
                    )
                }
            closeSlotSheet()
        }
    }

    fun requestMealSuggestion() {
        val slot = _activeSheet.value ?: return
        if (_isFetchingSuggestion.value) return
        viewModelScope.launch {
            _isFetchingSuggestion.value = true
            val targets = macroTargets.value
            val log = dayLog.value
            val consumedProtein = log.slots.values.sumOf { it.totalProtein } - log.slotLog(slot).totalProtein
            val remainingProtein = (targets.proteinG - consumedProtein).coerceAtLeast(0.0)
            val foods = foodRepository.getRecentFoods().first().map { it.name }
            aiRepository.getMealSuggestion(slot, remainingProtein, foods)
                .onSuccess { _mealSuggestion.value = it }
            _isFetchingSuggestion.value = false
        }
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
