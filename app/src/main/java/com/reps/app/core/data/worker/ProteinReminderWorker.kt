package com.reps.app.core.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reps.app.R
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import com.reps.app.core.domain.MacroCalculator
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.repository.MealLogRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class ProteinReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appSettings: AppSettingsDataStore,
    private val userPrefs: UserPreferencesDataStore,
    private val mealLogRepository: MealLogRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!appSettings.proteinRemindersEnabled.first()) return Result.success()

        val today = LocalDate.now().toString()
        val dayLog = mealLogRepository.getDayLog(today).first()
        val proteinConsumed = dayLog.totalProtein

        val weightKg = userPrefs.weightKg.first()
        val heightCm = userPrefs.heightCm.first()
        val age = userPrefs.age.first()
        val activity = userPrefs.activityLevel.first()

        val targets = if (weightKg > 0 && heightCm > 0 && age > 0) {
            MacroCalculator.compute(weightKg, heightCm, age, activity)
        } else {
            MacroTargets.DEFAULT
        }

        if (proteinConsumed >= targets.proteinG * 0.85) return Result.success()

        showReminderNotification(targets.proteinG, proteinConsumed.toInt())
        return Result.success()
    }

    private fun showReminderNotification(targetG: Int, consumedG: Int) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.protein_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = applicationContext.getString(R.string.protein_reminder_channel_desc)
        }
        manager.createNotificationChannel(channel)

        val remaining = (targetG - consumedG).coerceAtLeast(0)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.protein_reminder_title))
            .setContentText(applicationContext.getString(R.string.protein_reminder_body, remaining))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "protein_reminder_worker"
        const val CHANNEL_ID = "protein_reminder_channel"
        private const val NOTIFICATION_ID = 1002
    }
}
