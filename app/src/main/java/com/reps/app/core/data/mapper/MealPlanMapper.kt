package com.reps.app.core.data.mapper

import com.reps.app.core.data.relation.MealPlanTemplateWithSlots
import com.reps.app.core.domain.model.GroceryCategory
import com.reps.app.core.domain.model.MealPlanDayPlan
import com.reps.app.core.domain.model.MealPlanSlotFood
import com.reps.app.core.domain.model.MealPlanTemplate
import com.reps.app.core.domain.model.MealSlot

private val DAY_LABELS = mapOf(
    0 to "Mon / Thu",
    1 to "Tue / Fri",
    2 to "Wed / Sat",
    3 to "Sunday"
)

fun MealPlanTemplateWithSlots.toDomain(): MealPlanTemplate {
    val slotsByDay = slots.groupBy { it.slot.dayOfWeek }
    val days = slotsByDay.keys.sorted().map { dayIndex ->
        val daySlots = slotsByDay[dayIndex] ?: emptyList()
        val byMealType = daySlots
            .groupBy { it.slot.mealType }
            .mapKeys { (mealType, _) ->
                runCatching { MealSlot.valueOf(mealType) }.getOrElse { MealSlot.SNACKS }
            }
            .mapValues { (_, items) ->
                items.map { swf ->
                    MealPlanSlotFood(
                        slotId = swf.slot.id,
                        food = swf.food.toDomainModel(),
                        servingMultiplier = swf.slot.servingMultiplier,
                        sortOrder = swf.slot.sortOrder
                    )
                }.sortedBy { it.sortOrder }
            }
        MealPlanDayPlan(
            dayIndex = dayIndex,
            label = DAY_LABELS[dayIndex] ?: "Day ${dayIndex + 1}",
            slots = byMealType
        )
    }
    return MealPlanTemplate(
        id = template.id,
        name = template.name,
        description = template.description,
        cuisineType = template.cuisineType,
        days = days,
        isCustom = template.isCustom
    )
}

fun groceryCategoryFor(name: String): GroceryCategory {
    val n = name.lowercase()
    return when {
        n.contains("chicken") || n.contains("egg") || n.contains("fish") ||
        n.contains("paneer") || n.contains("soya") || n.contains("tofu") ||
        n.contains("whey") || n.contains("tuna") || n.contains("turkey") ||
        n.contains("mutton") || n.contains("prawn") || n.contains("dal") ||
        n.contains("lentil") || n.contains("bean") -> GroceryCategory.PROTEIN

        n.contains("milk") || n.contains("curd") || n.contains("yogurt") ||
        n.contains("butter") || n.contains("ghee") || n.contains("cream") ||
        n.contains("cheese") || n.contains("lassi") -> GroceryCategory.DAIRY

        n.contains("spinach") || n.contains("broccoli") || n.contains("tomato") ||
        n.contains("onion") || n.contains("banana") || n.contains("apple") ||
        n.contains("vegetable") || n.contains("salad") || n.contains("fruit") ||
        n.contains("carrot") || n.contains("palak") || n.contains("capsicum") ||
        n.contains("mushroom") -> GroceryCategory.PRODUCE

        n.contains("frozen") -> GroceryCategory.FROZEN

        else -> GroceryCategory.PANTRY
    }
}
