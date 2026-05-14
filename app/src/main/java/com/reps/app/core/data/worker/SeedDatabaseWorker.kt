package com.reps.app.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reps.app.core.data.RepsDatabase
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.data.mapper.toEntity
import com.reps.app.core.network.dto.FoodItemDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

@HiltWorker
class SeedDatabaseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: RepsDatabase,
    private val appSettingsDataStore: AppSettingsDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (appSettingsDataStore.isDbSeeded.first()) return Result.success()

        return try {
            val json = applicationContext.assets.open("indian_foods.json")
                .bufferedReader()
                .readText()
            val dtos = Json { ignoreUnknownKeys = true }
                .decodeFromString<List<FoodItemDto>>(json)
            database.foodItemDao().insertAll(dtos.map { it.toEntity() })
            appSettingsDataStore.setDbSeeded(true)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "seed_database"
        private const val MAX_RETRIES = 3
    }
}
