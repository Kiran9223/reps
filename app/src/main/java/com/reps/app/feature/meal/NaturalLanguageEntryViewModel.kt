package com.reps.app.feature.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.ParsedFoodEntry
import com.reps.app.core.domain.RuleBasedFoodParser
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.repository.MealLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NlEntryUiState(
    val input: String = "",
    val isParsing: Boolean = false,
    val parsedEntries: List<ParsedFoodEntry> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NaturalLanguageEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val parser: RuleBasedFoodParser,
    private val mealLogRepository: MealLogRepository
) : ViewModel() {

    private val date: String = checkNotNull(savedStateHandle["date"])
    val slot: MealSlot = MealSlot.valueOf(checkNotNull(savedStateHandle["slot"]))

    private val _uiState = MutableStateFlow(NlEntryUiState())
    val uiState: StateFlow<NlEntryUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text, parsedEntries = emptyList(), error = null) }
    }

    fun onParse() {
        val input = _uiState.value.input.trim()
        if (input.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isParsing = true, error = null, parsedEntries = emptyList()) }
            val entries = parser.parse(input)
            _uiState.update {
                it.copy(
                    isParsing = false,
                    parsedEntries = entries,
                    error = if (entries.isEmpty()) "no_matches" else null
                )
            }
        }
    }

    fun onLogIt() {
        val entries = _uiState.value.parsedEntries
        if (entries.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            entries.forEach { entry ->
                runCatching {
                    mealLogRepository.addFoodToLog(date, slot, entry.food.id, entry.quantity)
                }
            }
            _uiState.update { it.copy(isSaving = false, saved = true) }
        }
    }
}
