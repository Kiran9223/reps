package com.reps.app.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reps.app.ai.AIRepository
import com.reps.app.core.data.dao.WorkoutLogDao
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import com.reps.app.core.domain.MacroCalculator
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.repository.MealLogRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

@HiltWorker
class DailyInsightWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val aiRepository: AIRepository,
    private val mealLogRepository: MealLogRepository,
    private val userPrefs: UserPreferencesDataStore,
    private val appSettings: AppSettingsDataStore,
    private val workoutLogDao: WorkoutLogDao
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_insight_worker"
    }

    override suspend fun doWork(): Result {
        val existingInsight = appSettings.dailyInsight.first()
        if (existingInsight != null) return Result.success()

        val today = LocalDate.now()
        val todayStr = today.toString()
        val dayLog = mealLogRepository.getDayLog(todayStr).first()

        val weightKg = userPrefs.weightKg.first()
        val heightCm = userPrefs.heightCm.first()
        val age = userPrefs.age.first()
        val activity = userPrefs.activityLevel.first()
        val targetWeight = userPrefs.targetWeightKg.first()

        val targets = if (weightKg > 0 && heightCm > 0 && age > 0)
            MacroCalculator.compute(weightKg, heightCm, age, activity)
        else MacroTargets.DEFAULT

        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todayWorkouts = workoutLogDao.getByDateRange(startOfDay, endOfDay).first()
        val workoutDone = todayWorkouts.isNotEmpty()

        val waterMl = appSettings.getWaterMl(todayStr).first()

        aiRepository.getDailyInsight(dayLog, targets, weightKg, targetWeight, workoutDone, waterMl)
            .onSuccess { insight -> appSettings.saveDailyInsight(insight) }

        return Result.success()
    }
}
