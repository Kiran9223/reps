package com.reps.app.core.data.repository

import com.reps.app.core.data.dao.BodyMeasurementDao
import com.reps.app.core.data.dao.WeightLogDao
import com.reps.app.core.data.dao.WorkoutLogDao
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import com.reps.app.core.data.entity.BodyMeasurementEntity
import com.reps.app.core.data.entity.WeightLogEntity
import com.reps.app.core.data.entity.WorkoutLogEntity
import com.reps.app.core.data.mapper.toBodyMeasurement
import com.reps.app.core.data.mapper.toWeightCheckIn
import com.reps.app.core.domain.MacroCalculator
import com.reps.app.core.domain.model.BodyMeasurement
import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.Streaks
import com.reps.app.core.domain.model.WeeklyStats
import com.reps.app.core.domain.model.WeightCheckIn
import com.reps.app.core.domain.repository.MealLogRepository
import com.reps.app.core.domain.repository.ProgressRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val weightLogDao: WeightLogDao,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val workoutLogDao: WorkoutLogDao,
    private val mealLogRepository: MealLogRepository,
    private val userPrefs: UserPreferencesDataStore,
    @com.reps.app.core.di.IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ProgressRepository {

    override suspend fun logWeight(date: Long, weightKg: Double, notes: String?) {
        withContext(ioDispatcher) {
            weightLogDao.insert(WeightLogEntity(date = date, weightKg = weightKg, notes = notes))
        }
    }

    override fun getWeightHistory(): Flow<List<WeightCheckIn>> =
        weightLogDao.getAll().map { entities ->
            val asc = entities.sortedBy { it.date }
            asc.mapIndexed { i, entity ->
                val delta = if (i > 0) entity.weightKg - asc[i - 1].weightKg else null
                entity.toWeightCheckIn(delta)
            }.reversed()
        }

    override suspend fun logMeasurements(
        date: Long,
        waistCm: Double?,
        chestCm: Double?,
        armsCm: Double?,
        thighsCm: Double?,
        notes: String?
    ) {
        withContext(ioDispatcher) {
            bodyMeasurementDao.insert(
                BodyMeasurementEntity(
                    date = date,
                    waistCm = waistCm,
                    chestCm = chestCm,
                    armsCm = armsCm,
                    thighsCm = thighsCm,
                    notes = notes
                )
            )
        }
    }

    override fun getMeasurementHistory(): Flow<List<BodyMeasurement>> =
        bodyMeasurementDao.getAll().map { entities ->
            // entities are DESC (most recent first); prev = index+1
            entities.mapIndexed { i, entity ->
                entity.toBodyMeasurement(prev = entities.getOrNull(i + 1))
            }
        }

    override fun getStreaks(): Flow<Streaks> {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
        val macroFlow = combine(
            userPrefs.weightKg, userPrefs.heightCm, userPrefs.age, userPrefs.activityLevel
        ) { weight, height, age, activity ->
            if (weight > 0 && height > 0 && age > 0)
                MacroCalculator.compute(weight, height, age, activity)
            else MacroTargets.DEFAULT
        }
        return combine(
            mealLogRepository.getLogHistory(30),
            workoutLogDao.getByDateRange(thirtyDaysAgo, System.currentTimeMillis()),
            macroFlow
        ) { history, workouts, targets ->
            Streaks(
                proteinStreakDays = computeProteinStreak(history, targets.proteinG.toDouble()),
                workoutStreakDays = computeWorkoutStreak(workouts),
                waterStreakDays = 0
            )
        }
    }

    override fun getWeeklyStats(): Flow<WeeklyStats> {
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
        val macroFlow = combine(
            userPrefs.weightKg, userPrefs.heightCm, userPrefs.age, userPrefs.activityLevel
        ) { weight, height, age, activity ->
            if (weight > 0 && height > 0 && age > 0)
                MacroCalculator.compute(weight, height, age, activity)
            else MacroTargets.DEFAULT
        }
        return combine(
            mealLogRepository.getLogHistory(7),
            workoutLogDao.getByDateRange(sevenDaysAgo, System.currentTimeMillis()),
            macroFlow,
            userPrefs.workoutDaysPerWeek
        ) { history, workouts, targets, workoutTarget ->
            WeeklyStats(
                avgCaloriesKcal = if (history.isEmpty()) 0
                    else history.map { it.totalCalories }.average().toInt(),
                avgProteinG = if (history.isEmpty()) 0.0
                    else history.map { it.totalProtein }.average(),
                workoutsDone = workouts.size,
                workoutTargetPerWeek = workoutTarget,
                proteinGoalDays = history.count { it.totalProtein >= targets.proteinG }
            )
        }
    }

    private fun computeProteinStreak(history: List<DayLog>, proteinTargetG: Double): Int {
        if (history.isEmpty()) return 0
        val logsByDate = history.associateBy { it.date }
        val today = LocalDate.now()
        var streak = 0
        var checkDate = today
        while (true) {
            val dayLog = logsByDate[checkDate.toString()]
            if (dayLog != null && dayLog.totalProtein >= proteinTargetG) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else break
        }
        return streak
    }

    private fun computeWorkoutStreak(workouts: List<WorkoutLogEntity>): Int {
        if (workouts.isEmpty()) return 0
        val workoutDaySet = workouts.map { entity ->
            Instant.ofEpochMilli(entity.date).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
        val today = LocalDate.now()
        var streak = 0
        var checkDate = today
        while (workoutDaySet.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        return streak
    }

    companion object {
        fun projectWeeksToGoal(history: List<WeightCheckIn>, targetKg: Double): Int? {
            if (history.size < 3) return null
            val cutoff = System.currentTimeMillis() - 28L * 24 * 3600 * 1000
            val recent = history.filter { it.date >= cutoff }.sortedBy { it.date }
            if (recent.size < 2) return null
            val n = recent.size.toDouble()
            val sumX = (0 until recent.size).sumOf { it.toDouble() }
            val sumY = recent.sumOf { it.weightKg }
            val sumXY = recent.mapIndexed { i, c -> i * c.weightKg }.sum()
            val sumX2 = (0 until recent.size).sumOf { (it * it).toDouble() }
            val denom = n * sumX2 - sumX * sumX
            if (denom == 0.0) return null
            val slope = (n * sumXY - sumX * sumY) / denom
            if (slope >= -0.01) return null
            val current = recent.last().weightKg
            val kgRemaining = current - targetKg
            if (kgRemaining <= 0) return 0
            val daysToGoal = (kgRemaining / (-slope)).toInt()
            return (daysToGoal / 7).coerceIn(1, 104)
        }
    }
}
