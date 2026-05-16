package com.reps.app.feature.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.FoodItem
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FoodTab { ALL, INDIAN, RECENT, CUSTOM }

data class FoodSearchUiState(
    val query: String = "",
    val activeTab: FoodTab = FoodTab.ALL,
    val foods: List<FoodItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val slotDisplayName: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val slotName: String? = savedStateHandle.get<String>("slot")?.takeIf { it.isNotBlank() }
    private val slotDisplayName: String? = slotName?.let {
        runCatching { MealSlot.valueOf(it).displayName }.getOrNull()
    }

    private val _uiState = MutableStateFlow(FoodSearchUiState(slotDisplayName = slotDisplayName))
    val uiState: StateFlow<FoodSearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    private val _activeTab = MutableStateFlow(FoodTab.ALL)
    private val _retryTrigger = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(_query.debounce(300L), _activeTab, _retryTrigger) { q, t, _ -> q to t }
                .collectLatest { (query, tab) -> loadFoods(query, tab) }
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
        _uiState.update { it.copy(query = query) }
    }

    fun onTabChange(tab: FoodTab) {
        _activeTab.value = tab
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun retry() { _retryTrigger.value++ }

    private suspend fun loadFoods(query: String, tab: FoodTab) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val baseFlow: Flow<List<FoodItem>> = when (tab) {
                FoodTab.ALL -> if (query.isNotBlank()) foodRepository.searchFoods(query)
                              else foodRepository.getRecentFoods()
                FoodTab.INDIAN -> foodRepository.getFoodsByCuisineTag("indian")
                FoodTab.RECENT -> foodRepository.getRecentFoods()
                FoodTab.CUSTOM -> foodRepository.getCustomFoods()
            }
            val resultFlow: Flow<List<FoodItem>> =
                if (tab != FoodTab.ALL && query.isNotBlank()) {
                    baseFlow.map { foods ->
                        foods.filter { it.name.contains(query, ignoreCase = true) }
                    }
                } else baseFlow

            resultFlow.collect { foods ->
                _uiState.update { it.copy(foods = foods, isLoading = false) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Unable to load foods") }
        }
    }
}
