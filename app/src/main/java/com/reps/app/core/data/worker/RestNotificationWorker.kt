package com.reps.app.core.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reps.app.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RestNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        showRestCompleteNotification()
        return Result.success()
    }

    private fun showRestCompleteNotification() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rest Timer",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when rest period is complete"
        }
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Rest complete!")
            .setContentText("Time to hit your next set.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "rest_notification"
        const val CHANNEL_ID = "rest_timer_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
