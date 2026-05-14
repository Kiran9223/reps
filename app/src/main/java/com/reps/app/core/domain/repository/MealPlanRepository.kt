package com.reps.app.core.domain.repository

import com.reps.app.core.domain.model.GroceryList
import com.reps.app.core.domain.model.MealPlanTemplate
import com.reps.app.core.domain.model.MealSlot
import kotlinx.coroutines.flow.Flow

interface MealPlanRepository {
    fun getActiveMealPlan(): Flow<MealPlanTemplate?>
    fun getAllTemplates(): Flow<List<MealPlanTemplate>>
    fun getGroceryList(templateId: Long): Flow<GroceryList>
    suspend fun setActivePlan(templateId: Long)
    suspend fun cloneTemplate(templateId: Long): Long
    suspend fun updateSlotFood(slotId: Long, foodItemId: Long, servingMultiplier: Double)
    suspend fun toggleGroceryItemBought(templateId: Long, foodId: Long)

    suspend fun createPlan(
        name: String,
        description: String?,
        cuisineType: String,
        draftSlots: Map<Int, Map<MealSlot, List<Pair<Long, Double>>>>
    ): Long

    suspend fun updatePlan(
        templateId: Long,
        name: String,
        description: String?,
        cuisineType: String,
        draftSlots: Map<Int, Map<MealSlot, List<Pair<Long, Double>>>>
    )

    suspend fun deletePlan(templateId: Long)
}
