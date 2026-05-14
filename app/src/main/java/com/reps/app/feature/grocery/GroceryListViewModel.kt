package com.reps.app.feature.grocery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.GroceryCategory
import com.reps.app.core.domain.model.GroceryItem
import com.reps.app.core.domain.model.GroceryList
import com.reps.app.core.domain.repository.MealPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroceryListUiState(
    val isNoPlan: Boolean = false,
    val groceryList: GroceryList? = null,
    val customItemInput: String = ""
)

@HiltViewModel
class GroceryListViewModel @Inject constructor(
    private val mealPlanRepository: MealPlanRepository
) : ViewModel() {

    private val _customItems = MutableStateFlow<List<GroceryItem>>(emptyList())
    private val _customItemInput = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GroceryListUiState> = mealPlanRepository.getActiveMealPlan()
        .flatMapLatest { plan ->
            if (plan == null) {
                flowOf(GroceryListUiState(isNoPlan = true))
            } else {
                combine(
                    mealPlanRepository.getGroceryList(plan.id),
                    _customItems,
                    _customItemInput
                ) { list, customItems, input ->
                    val merged = list.itemsByCategory.toMutableMap()
                    if (customItems.isNotEmpty()) {
                        merged[GroceryCategory.PANTRY] =
                            (merged[GroceryCategory.PANTRY] ?: emptyList()) + customItems
                    }
                    GroceryListUiState(
                        groceryList = list.copy(itemsByCategory = merged),
                        customItemInput = input
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroceryListUiState())

    fun setCustomItemInput(input: String) {
        _customItemInput.value = input
    }

    fun addCustomItem() {
        val name = _customItemInput.value.trim()
        if (name.isBlank()) return
        _customItems.value = _customItems.value + GroceryItem(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            quantity = 1.0,
            servingDescription = "",
            category = GroceryCategory.PANTRY,
            isBought = false,
            isCustom = true
        )
        _customItemInput.value = ""
    }

    fun toggleBought(templateId: Long, item: GroceryItem) {
        if (item.isCustom) {
            _customItems.value = _customItems.value.map {
                if (it.id == item.id) it.copy(isBought = !it.isBought) else it
            }
        } else {
            val foodId = item.id.substringAfterLast("_").toLongOrNull() ?: return
            viewModelScope.launch {
                mealPlanRepository.toggleGroceryItemBought(templateId, foodId)
            }
        }
    }

    fun buildShareText(list: GroceryList): String = buildString {
        appendLine("Grocery List — ${list.templateName}")
        appendLine()
        GroceryCategory.entries.forEach { cat ->
            val items = list.itemsByCategory[cat]
            if (!items.isNullOrEmpty()) {
                appendLine("${cat.displayName}:")
                items.forEach { item ->
                    val check = if (item.isBought) "✓" else "○"
                    val qty = item.quantity.toInt().takeIf { it > 1 }?.let { " ×$it" } ?: ""
                    appendLine("  $check ${item.name}$qty")
                }
                appendLine()
            }
        }
    }
}
