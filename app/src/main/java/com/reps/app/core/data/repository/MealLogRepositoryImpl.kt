package com.reps.app.core.data.repository

import com.reps.app.core.data.dao.MealLogDao
import com.reps.app.core.data.dao.MealLogEntryDao
import com.reps.app.core.data.entity.MealLogEntity
import com.reps.app.core.data.entity.MealLogEntryEntity
import com.reps.app.core.data.mapper.toDomainModel
import com.reps.app.core.data.relation.MealLogWithEntries
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.DayMacros
import com.reps.app.core.domain.model.LoggedFood
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.model.MealSlotLog
import com.reps.app.core.domain.repository.MealLogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealLogRepositoryImpl @Inject constructor(
    private val mealLogDao: MealLogDao,
    private val mealLogEntryDao: MealLogEntryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MealLogRepository {

    override fun getTodayLog(): Flow<DayLog> = getDayLog(LocalDate.now().toString())

    override fun getDayLog(dateStr: String): Flow<DayLog> {
        val epochMillis = LocalDate.parse(dateStr).toEpochDay() * 86_400_000L
        return mealLogDao.getWithEntriesForDate(epochMillis)
            .map { logs -> buildDayLog(dateStr, logs) }
            .flowOn(ioDispatcher)
    }

    override suspend fun addFoodToLog(date: String, slot: MealSlot, foodItemId: Long, servings: Double) {
        withContext(ioDispatcher) {
            val epochMillis = LocalDate.parse(date).toEpochDay() * 86_400_000L
            val existing = mealLogDao.getByDateAndType(epochMillis, slot.name)
            val mealLogId = existing?.id ?: mealLogDao.insert(
                MealLogEntity(date = epochMillis, mealType = slot.name)
            )
            mealLogEntryDao.insert(
                MealLogEntryEntity(
                    mealLogId = mealLogId,
                    foodItemId = foodItemId,
                    servingMultiplier = servings
                )
            )
        }
    }

    override suspend fun removeFoodFromLog(entryId: Long) {
        withContext(ioDispatcher) { mealLogEntryDao.softDelete(entryId) }
    }

    override suspend fun updateServings(entryId: Long, servings: Double) {
        withContext(ioDispatcher) {
            val entry = mealLogEntryDao.getById(entryId) ?: return@withContext
            mealLogEntryDao.update(entry.copy(servingMultiplier = servings))
        }
    }

    override fun getDayMacros(dateStr: String, targets: MacroTargets): Flow<DayMacros> =
        getDayLog(dateStr).map { log ->
            DayMacros(
                calories = log.totalCalories,
                protein = log.totalProtein,
                carbs = log.totalCarbs,
                fat = log.totalFat,
                targets = targets
            )
        }

    override fun getLogHistory(days: Int): Flow<List<DayLog>> {
        val today = LocalDate.now()
        val endEpoch = today.toEpochDay() * 86_400_000L
        val startEpoch = today.minusDays(days.toLong() - 1).toEpochDay() * 86_400_000L
        return mealLogDao.getWithEntriesForDateRange(startEpoch, endEpoch)
            .map { allLogs ->
                val grouped = allLogs.groupBy { it.log.date }
                (0 until days).map { daysAgo ->
                    val date = today.minusDays(daysAgo.toLong())
                    val dateStr = date.toString()
                    val epochMillis = date.toEpochDay() * 86_400_000L
                    buildDayLog(dateStr, grouped[epochMillis] ?: emptyList())
                }
            }
            .flowOn(ioDispatcher)
    }

    private fun buildDayLog(dateStr: String, logs: List<MealLogWithEntries>): DayLog {
        val slots = MealSlot.entries.associateWith { slot ->
            val logWithEntries = logs.find { it.log.mealType == slot.name }
            MealSlotLog(
                logId = logWithEntries?.log?.id,
                slot = slot,
                entries = logWithEntries?.entries
                    ?.filter { !it.entry.isDeleted }
                    ?.map { entryWithFood ->
                        LoggedFood(
                            entryId = entryWithFood.entry.id,
                            food = entryWithFood.food.toDomainModel(),
                            servingMultiplier = entryWithFood.entry.servingMultiplier
                        )
                    } ?: emptyList()
            )
        }
        return DayLog(date = dateStr, slots = slots)
    }
}
