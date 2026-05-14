package com.reps.app.core.domain.model

data class DayLog(
    val date: String,          // ISO format "2025-05-14"
    val slots: Map<MealSlot, MealSlotLog> = emptyMap()
) {
    val totalCalories: Double get() = slots.values.sumOf { it.totalCalories }
    val totalProtein: Double get() = slots.values.sumOf { it.totalProtein }
    val totalCarbs: Double get() = slots.values.sumOf { it.totalCarbs }
    val totalFat: Double get() = slots.values.sumOf { it.totalFat }

    fun slotLog(slot: MealSlot): MealSlotLog =
        slots[slot] ?: MealSlotLog(logId = null, slot = slot)
}
