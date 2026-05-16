package com.reps.app.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.BodyMeasurement
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

data class BodyMeasurementsUiState(
    val history: List<BodyMeasurement> = emptyList(),
    val waistText: String = "",
    val chestText: String = "",
    val armsText: String = "",
    val thighsText: String = "",
    val notesText: String = "",
    val saveError: String? = null,
    val isSaving: Boolean = false,
    val showMotivational: Boolean = false
)

@HiltViewModel
class BodyMeasurementsViewModel @Inject constructor(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _waist = MutableStateFlow("")
    private val _chest = MutableStateFlow("")
    private val _arms = MutableStateFlow("")
    private val _thighs = MutableStateFlow("")
    private val _notes = MutableStateFlow("")
    private val _saveError = MutableStateFlow<String?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _showMotivational = MutableStateFlow(false)

    val uiState: StateFlow<BodyMeasurementsUiState> = combine(
        progressRepository.getMeasurementHistory(),
        combine(_waist, _chest, _arms, _thighs) { w, c, a, t -> listOf(w, c, a, t) },
        combine(_notes, _saveError, _isSaving, _showMotivational) { n, err, saving, mot ->
            listOf<Any?>(n, err, saving, mot)
        }
    ) { history, inputs, extras ->
        BodyMeasurementsUiState(
            history = history,
            waistText = inputs[0],
            chestText = inputs[1],
            armsText = inputs[2],
            thighsText = inputs[3],
            notesText = extras[0] as String,
            saveError = extras[1] as String?,
            isSaving = extras[2] as Boolean,
            showMotivational = extras[3] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), BodyMeasurementsUiState())

    fun onWaistChanged(v: String) { _waist.value = v; _saveError.value = null }
    fun onChestChanged(v: String) { _chest.value = v; _saveError.value = null }
    fun onArmsChanged(v: String) { _arms.value = v; _saveError.value = null }
    fun onThighsChanged(v: String) { _thighs.value = v; _saveError.value = null }
    fun onNotesChanged(v: String) { _notes.value = v }

    fun save() {
        val waist = _waist.value.toDoubleOrNull()
        val chest = _chest.value.toDoubleOrNull()
        val arms = _arms.value.toDoubleOrNull()
        val thighs = _thighs.value.toDoubleOrNull()
        if (waist == null && chest == null && arms == null && thighs == null) {
            _saveError.value = "Enter at least one measurement"
            return
        }
        _isSaving.value = true
        viewModelScope.launch {
            try {
                progressRepository.logMeasurements(
                    date = System.currentTimeMillis(),
                    waistCm = waist,
                    chestCm = chest,
                    armsCm = arms,
                    thighsCm = thighs,
                    notes = _notes.value.trim().ifBlank { null }
                )
                _waist.value = ""
                _chest.value = ""
                _arms.value = ""
                _thighs.value = ""
                _notes.value = ""
                _showMotivational.value = (waist != null || chest != null)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun dismissMotivational() { _showMotivational.value = false }

    fun deleteMeasurement(id: Long) {
        viewModelScope.launch { progressRepository.deleteMeasurement(id) }
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
}
