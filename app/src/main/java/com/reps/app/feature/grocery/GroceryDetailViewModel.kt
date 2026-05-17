package com.reps.app.feature.grocery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.ai.AIRepository
import com.reps.app.core.data.mapper.groceryCategoryFor
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.model.GroceryCategory
import com.reps.app.core.domain.model.MealPlanTemplate
import com.reps.app.core.domain.model.SavedGroceryItem
import com.reps.app.core.domain.repository.GroceryRepository
import com.reps.app.core.domain.repository.MealPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GroceryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groceryRepository: GroceryRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val aiRepository: AIRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    val listId: Long = savedStateHandle["listId"] ?: -1L

    private val _listName = MutableStateFlow("")
    val listName: StateFlow<String> = _listName.asStateFlow()

    private val _itemInput = MutableStateFlow("")
    val itemInput: StateFlow<String> = _itemInput.asStateFlow()

    private val _isCategorizing = MutableStateFlow(false)
    val isCategorizing: StateFlow<Boolean> = _isCategorizing.asStateFlow()

    val itemsByCategory: StateFlow<Map<GroceryCategory, List<SavedGroceryItem>>> =
        groceryRepository.getItems(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch(ioDispatcher) {
            _listName.value = groceryRepository.getListName(listId)
        }
    }

    fun onInputChange(text: String) {
        _itemInput.value = text
    }

    fun addItem() {
        val name = _itemInput.value.trim()
        if (name.isBlank()) return
        _itemInput.value = ""
        viewModelScope.launch {
            _isCategorizing.value = true
            val categoryName = withContext(ioDispatcher) {
                aiRepository.categorizeGroceryItem(name).getOrElse { "PANTRY" }
            }
            val category = runCatching { GroceryCategory.valueOf(categoryName) }
                .getOrElse { GroceryCategory.PANTRY }
            _isCategorizing.value = false
            withContext(ioDispatcher) {
                groceryRepository.addItem(listId, name, category)
            }
        }
    }

    fun toggleItem(itemId: Long) {
        viewModelScope.launch(ioDispatcher) {
            groceryRepository.toggleItem(itemId)
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch(ioDispatcher) {
            groceryRepository.deleteItem(itemId)
        }
    }

    fun addFromMealPlan() {
        viewModelScope.launch {
            val plan: MealPlanTemplate = withContext(ioDispatcher) {
                mealPlanRepository.getActiveMealPlan().first()
            } ?: return@launch

            val foodNames = plan.days
                .flatMap { it.slots.values.flatten() }
                .map { it.food.name }
                .distinct()

            foodNames.forEach { name ->
                val category = groceryCategoryFor(name)
                withContext(ioDispatcher) {
                    groceryRepository.addItem(listId, name, category)
                }
            }
        }
    }

    fun buildShareText(listName: String): String = buildString {
        appendLine("Grocery List — $listName")
        appendLine()
        val items = itemsByCategory.value
        GroceryCategory.entries.forEach { cat ->
            val catItems = items[cat]
            if (!catItems.isNullOrEmpty()) {
                appendLine("${cat.displayName}:")
                catItems.forEach { item ->
                    val check = if (item.isBought) "✓" else "○"
                    appendLine("  $check ${item.name}")
                }
                appendLine()
            }
        }
    }
}
