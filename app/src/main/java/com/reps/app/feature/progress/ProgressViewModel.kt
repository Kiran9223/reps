package com.reps.app.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import com.reps.app.core.data.repository.ProgressRepositoryImpl
import com.reps.app.core.domain.model.BodyMeasurement
import com.reps.app.core.domain.model.GoalProgress
import com.reps.app.core.domain.model.Streaks
import com.reps.app.core.domain.model.WeeklyStats
import com.reps.app.core.domain.model.WeightCheckIn
import com.reps.app.core.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ProgressUiState(
    val weightHistory: List<WeightCheckIn> = emptyList(),
    val measurements: List<BodyMeasurement> = emptyList(),
    val streaks: Streaks = Streaks(),
    val weeklyStats: WeeklyStats = WeeklyStats(),
    val goalProgress: GoalProgress? = null,
    val progressInsight: String? = null,
    val measurementsExpanded: Boolean = false,
    val showCheckInSheet: Boolean = false,
    val checkInWeightText: String = "",
    val checkInNotesText: String = "",
    val checkInError: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val userPrefs: UserPreferencesDataStore
) : ViewModel() {

    private val _showCheckInSheet = MutableStateFlow(false)
    private val _checkInWeight = MutableStateFlow("")
    private val _checkInNotes = MutableStateFlow("")
    private val _checkInError = MutableStateFlow<String?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _measurementsExpanded = MutableStateFlow(false)

    private val weightHistory: StateFlow<List<WeightCheckIn>> = progressRepository.getWeightHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val goalProgressFlow: StateFlow<GoalProgress?> = combine(
        userPrefs.weightKg,
        userPrefs.targetWeightKg,
        progressRepository.getWeightHistory()
    ) { startWeight, targetWeight, history ->
        buildGoalProgress(startWeight, targetWeight, history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    val uiState: StateFlow<ProgressUiState> = combine(
        combine(weightHistory, progressRepository.getMeasurementHistory()) { w, m -> w to m },
        combine(progressRepository.getStreaks(), progressRepository.getWeeklyStats()) { s, ws -> s to ws },
        combine(goalProgressFlow, _showCheckInSheet, _checkInWeight, _checkInNotes) { gp, sheet, weight, notes ->
            GoalSheetBundle(gp, sheet, weight, notes)
        },
        combine(_checkInError, _isSaving, _measurementsExpanded) { err, saving, expanded ->
            Triple(err, saving, expanded)
        }
    ) { (history, measurements), (streaks, weeklyStats), gsb, (err, saving, expanded) ->
        ProgressUiState(
            weightHistory = history,
            measurements = measurements,
            streaks = streaks,
            weeklyStats = weeklyStats,
            goalProgress = gsb.goalProgress,
            progressInsight = computeProgressInsight(gsb.goalProgress, streaks, weeklyStats),
            measurementsExpanded = expanded,
            showCheckInSheet = gsb.showSheet,
            checkInWeightText = gsb.weight,
            checkInNotesText = gsb.notes,
            checkInError = err,
            isSaving = saving
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ProgressUiState())

    fun openCheckInSheet() {
        _checkInWeight.value = ""
        _checkInNotes.value = ""
        _checkInError.value = null
        _showCheckInSheet.value = true
    }

    fun closeCheckInSheet() { _showCheckInSheet.value = false }

    fun onWeightChanged(text: String) {
        _checkInWeight.value = text
        _checkInError.value = null
    }

    fun onNotesChanged(text: String) { _checkInNotes.value = text }

    fun toggleMeasurements() { _measurementsExpanded.value = !_measurementsExpanded.value }

    fun saveCheckIn() {
        val weightText = _checkInWeight.value.trim()
        val weightKg = weightText.toDoubleOrNull()
        if (weightKg == null || weightKg <= 0) {
            _checkInError.value = "Enter a valid weight"
            return
        }
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                progressRepository.logWeight(now, weightKg, _checkInNotes.value.trim().ifBlank { null })
                _showCheckInSheet.value = false
                _checkInWeight.value = ""
                _checkInNotes.value = ""
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun formatDate(epochMillis: Long): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }

    private fun buildGoalProgress(
        startWeight: Double,
        targetWeight: Double,
        history: List<WeightCheckIn>
    ): GoalProgress? {
        if (startWeight <= 0 || targetWeight <= 0) return null
        val currentWeight = history.firstOrNull()?.weightKg ?: startWeight
        val isLosingWeight = targetWeight < startWeight
        val totalDelta = if (isLosingWeight) startWeight - targetWeight else targetWeight - startWeight
        val achieved = if (isLosingWeight) (startWeight - currentWeight).coerceAtLeast(0.0)
                        else (currentWeight - startWeight).coerceAtLeast(0.0)
        val fraction = if (totalDelta > 0) (achieved / totalDelta).toFloat().coerceIn(0f, 1f) else 1f
        val remaining = if (isLosingWeight) (currentWeight - targetWeight).coerceAtLeast(0.0)
                        else (targetWeight - currentWeight).coerceAtLeast(0.0)
        val weeksToGoal = ProgressRepositoryImpl.projectWeeksToGoal(history, targetWeight)
        return GoalProgress(
            startWeightKg = startWeight,
            currentWeightKg = currentWeight,
            targetWeightKg = targetWeight,
            kgRemaining = remaining,
            progressFraction = fraction,
            estimatedWeeksToGoal = weeksToGoal
        )
    }

    private fun computeProgressInsight(
        goalProgress: GoalProgress?,
        streaks: Streaks,
        weeklyStats: WeeklyStats
    ): String? {
        val gp = goalProgress ?: return null
        return when {
            gp.isGoalReached ->
                "You've reached your goal! Consider setting a new target to keep momentum."
            streaks.proteinStreakDays >= 14 ->
                "${streaks.proteinStreakDays}-day protein streak — muscle preservation is on point. Keep it up!"
            streaks.proteinStreakDays >= 7 ->
                "${streaks.proteinStreakDays} days of consistent protein! Don't break the chain."
            gp.estimatedWeeksToGoal != null && gp.estimatedWeeksToGoal <= 4 ->
                "You're ${String.format("%.1f", gp.kgRemaining)}kg away — on track, ~${gp.estimatedWeeksToGoal} weeks to go!"
            weeklyStats.proteinGoalDays >= 5 ->
                "You hit your protein goal ${weeklyStats.proteinGoalDays}/7 days this week. Great consistency!"
            gp.kgRemaining > 0 ->
                "${String.format("%.1f", gp.kgRemaining)}kg to goal. Log meals consistently to stay on track."
            else -> null
        }
    }

    private data class GoalSheetBundle(
        val goalProgress: GoalProgress?,
        val showSheet: Boolean,
        val weight: String,
        val notes: String
    )
}
