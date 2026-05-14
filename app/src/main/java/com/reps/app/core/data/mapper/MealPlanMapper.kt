package com.reps.app.core.data.mapper

import com.reps.app.core.data.relation.MealPlanTemplateWithSlots
import com.reps.app.core.domain.model.GroceryCategory
import com.reps.app.core.domain.model.GroceryItem
import com.reps.app.core.domain.model.GroceryList
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

private val DAY_OCCURRENCES = mapOf(0 to 2, 1 to 2, 2 to 2, 3 to 1)

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

fun MealPlanTemplateWithSlots.toGroceryList(boughtKeys: Set<String>): GroceryList {
    data class Agg(val name: String, val servingDescription: String, var totalMultiplier: Double = 0.0)

    val agg = mutableMapOf<Long, Agg>()
    for (swf in slots) {
        val occurrences = DAY_OCCURRENCES[swf.slot.dayOfWeek] ?: 1
        val existing = agg[swf.food.id]
        if (existing != null) {
            existing.totalMultiplier += swf.slot.servingMultiplier * occurrences
        } else {
            agg[swf.food.id] = Agg(
                name = swf.food.name,
                servingDescription = swf.food.servingDescription,
                totalMultiplier = swf.slot.servingMultiplier * occurrences
            )
        }
    }

    val items = agg.entries.map { (foodId, entry) ->
        val boughtKey = "${template.id}_$foodId"
        GroceryItem(
            id = boughtKey,
            name = entry.name,
            quantity = entry.totalMultiplier,
            servingDescription = entry.servingDescription,
            category = groceryCategoryFor(entry.name),
            isBought = boughtKey in boughtKeys,
            isCustom = false
        )
    }

    return GroceryList(
        templateId = template.id,
        templateName = template.name,
        itemsByCategory = items.groupBy { it.category }
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
