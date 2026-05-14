package com.reps.app.feature.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.model.MealPlanTemplate
import com.reps.app.core.domain.repository.MealPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class CookGroup(val displayName: String) {
    PROTEIN("Proteins to Cook"),
    CARB("Carbs to Prep"),
    CURRY("Curries & Sides"),
    SNACK("Snacks & Prep")
}

data class CookSession(
    val label: String,
    val description: String,
    val items: Map<CookGroup, List<String>>
)

data class BatchCookUiState(
    val isNoPlan: Boolean = false,
    val sundaySession: CookSession? = null,
    val wednesdaySession: CookSession? = null
)

@HiltViewModel
class BatchCookViewModel @Inject constructor(
    mealPlanRepository: MealPlanRepository
) : ViewModel() {

    val uiState: StateFlow<BatchCookUiState> = mealPlanRepository.getActiveMealPlan()
        .map { plan ->
            if (plan == null) {
                BatchCookUiState(isNoPlan = true)
            } else {
                BatchCookUiState(
                    sundaySession = buildSession(
                        label = "Sunday Cook Session",
                        description = "Covers Monday – Wednesday meals",
                        plan = plan,
                        dayIndices = listOf(0, 1)
                    ),
                    wednesdaySession = buildSession(
                        label = "Wednesday Prep Session",
                        description = "Covers Thursday – Sunday meals",
                        plan = plan,
                        dayIndices = listOf(2, 3)
                    )
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BatchCookUiState())

    private fun buildSession(
        label: String,
        description: String,
        plan: MealPlanTemplate,
        dayIndices: List<Int>
    ): CookSession {
        val groups = mutableMapOf<CookGroup, MutableList<String>>()
        val seen = mutableSetOf<Long>()

        for (dayIndex in dayIndices) {
            val day = plan.days.getOrNull(dayIndex) ?: continue
            day.slots.values.flatten().forEach { slotFood ->
                if (slotFood.food.id in seen) return@forEach
                seen.add(slotFood.food.id)
                val group = cookGroupFor(slotFood.food.name)
                groups.getOrPut(group) { mutableListOf() }.add(slotFood.food.name)
            }
        }

        return CookSession(label = label, description = description, items = groups)
    }

    private fun cookGroupFor(name: String): CookGroup {
        val n = name.lowercase()
        return when {
            n.contains("chicken") || n.contains("egg") || n.contains("fish") ||
            n.contains("paneer") || n.contains("dal") || n.contains("soya") ||
            n.contains("mutton") || n.contains("prawn") || n.contains("tofu") -> CookGroup.PROTEIN

            n.contains("rice") || n.contains("oats") || n.contains("roti") ||
            n.contains("chapati") || n.contains("upma") || n.contains("idli") ||
            n.contains("dosa") || n.contains("poha") || n.contains("biryani") -> CookGroup.CARB

            n.contains("sambar") || n.contains("curry") || n.contains("rasam") ||
            n.contains("chutney") || n.contains("gravy") || n.contains("sabzi") -> CookGroup.CURRY

            else -> CookGroup.SNACK
        }
    }
}
