package com.reps.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.reps.app.core.data.worker.SeedDatabaseWorker
import com.reps.app.core.data.worker.WgerSeedWorker
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
    }
}
