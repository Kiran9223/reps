package com.reps.app.core.domain.repository

import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.DayMacros
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import kotlinx.coroutines.flow.Flow

interface MealLogRepository {
    fun getTodayLog(): Flow<DayLog>
    fun getDayLog(dateStr: String): Flow<DayLog>
    suspend fun addFoodToLog(date: String, slot: MealSlot, foodItemId: Long, servings: Double)
    suspend fun removeFoodFromLog(entryId: Long)
    suspend fun updateServings(entryId: Long, servings: Double)
    fun getDayMacros(dateStr: String, targets: MacroTargets): Flow<DayMacros>
    fun getLogHistory(days: Int): Flow<List<DayLog>>
}
