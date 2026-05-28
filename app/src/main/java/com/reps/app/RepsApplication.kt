package com.reps.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.reps.app.core.data.worker.DailyInsightWorker
import com.reps.app.core.data.worker.ProteinReminderWorker
import com.reps.app.core.data.worker.SeedDatabaseWorker
import com.reps.app.core.data.worker.WgerSeedWorker
import androidx.work.Constraints
import java.util.Calendar
import java.util.concurrent.TimeUnit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class

RepsApplication : Application(), Configuration.Provider {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HiltWorkerFactoryEntryPoint {
        fun workerFactory(): HiltWorkerFactory
    }

    override val workManagerConfiguration: Configuration
        get() {
            val factory = EntryPointAccessors.fromApplication(
                this,
                HiltWorkerFactoryEntryPoint::class.java
            ).workerFactory()
            return Configuration.Builder()
                .setWorkerFactory(factory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        enqueueSeedWork()
    }

    private fun enqueueSeedWork() {
        val wm = WorkManager.getInstance(this)
        wm.enqueueUniqueWork(
            SeedDatabaseWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SeedDatabaseWorker>().build()
        )
        wm.enqueueUniqueWork(
            WgerSeedWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WgerSeedWorker>().build()
        )
        wm.enqueueUniquePeriodicWork(
            DailyInsightWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyInsightWorker>(1, TimeUnit.DAYS).build()
        )
        val proteinReminderRequest = PeriodicWorkRequestBuilder<ProteinReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelayToHour(17), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().build())
            .build()
        wm.enqueueUniquePeriodicWork(
            ProteinReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            proteinReminderRequest
        )
    }

    /** Delay until next occurrence of [hourOfDay] local time (default 5 PM). */
    private fun computeInitialDelayToHour(hourOfDay: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}
