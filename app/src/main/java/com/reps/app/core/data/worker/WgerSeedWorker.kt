package com.reps.app.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reps.app.core.data.RepsDatabase
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.data.entity.WorkoutTemplateEntity
import com.reps.app.core.data.entity.WorkoutTemplateExerciseEntity
import com.reps.app.core.data.mapper.toEntity
import com.reps.app.core.network.api.WgerApiService
import com.reps.app.core.network.dto.ExerciseSeedDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

@HiltWorker
class WgerSeedWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: RepsDatabase,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val wgerApiService: WgerApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (appSettingsDataStore.isExerciseSeeded.first()) return Result.success()
        return try {
            seedFromJson()
            tryFetchFromWger()
            seedWorkoutTemplates()
            appSettingsDataStore.setExerciseSeeded(true)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private suspend fun seedFromJson() {
        val raw = applicationContext.assets.open("exercises.json")
            .bufferedReader()
            .readText()
        val dtos = Json { ignoreUnknownKeys = true }
            .decodeFromString<List<ExerciseSeedDto>>(raw)
        database.exerciseDao().insertAll(dtos.map { it.toEntity() })
    }

    private suspend fun tryFetchFromWger() {
        try {
            val response = wgerApiService.getExercises(language = 2, limit = 100, offset = 0)
            val entities = response.results
                .filter { dto ->
                    val englishName = dto.translations.firstOrNull { it.language == 2 }?.name
                    !englishName.isNullOrBlank()
                }
                .map { it.toEntity() }
                .filter { entity ->
                    val existing = database.exerciseDao().searchImmediate(entity.name)
                    existing == null
                }
            if (entities.isNotEmpty()) {
                database.exerciseDao().insertAll(entities)
            }
        } catch (_: Exception) {
            // Network failure is non-fatal; local JSON data is sufficient
        }
    }

    private suspend fun seedWorkoutTemplates() {
        data class ExerciseSeed(val name: String, val sets: Int, val reps: String, val weightKg: Double? = null)
        data class TemplateSeed(val name: String, val description: String, val exercises: List<ExerciseSeed>)

        val templates = listOf(
            TemplateSeed(
                "Push Day",
                "Chest, triceps — horizontal push focus",
                listOf(
                    ExerciseSeed("Barbell Bench Press", 4, "6-8", 60.0),
                    ExerciseSeed("Incline Dumbbell Press", 3, "8-12", 20.0),
                    ExerciseSeed("Cable Crossover", 3, "12-15"),
                    ExerciseSeed("Tricep Pushdown (Cable)", 3, "12-15"),
                    ExerciseSeed("Skull Crusher", 3, "10-12", 20.0)
                )
            ),
            TemplateSeed(
                "Pull Day",
                "Back and biceps — vertical and horizontal pull",
                listOf(
                    ExerciseSeed("Pull-Up", 4, "6-10"),
                    ExerciseSeed("Barbell Bent-Over Row", 4, "6-8", 60.0),
                    ExerciseSeed("Seated Cable Row", 3, "10-12"),
                    ExerciseSeed("Lat Pulldown", 3, "10-12"),
                    ExerciseSeed("Barbell Bicep Curl", 3, "10-12", 25.0),
                    ExerciseSeed("Hammer Curl", 3, "12-15", 12.0)
                )
            ),
            TemplateSeed(
                "Leg Day",
                "Full lower body — quad, hamstring, glute, calf",
                listOf(
                    ExerciseSeed("Barbell Squat", 4, "5-8", 80.0),
                    ExerciseSeed("Romanian Deadlift", 3, "8-10", 60.0),
                    ExerciseSeed("Leg Press", 3, "10-12"),
                    ExerciseSeed("Leg Extension", 3, "12-15"),
                    ExerciseSeed("Lying Leg Curl", 3, "12-15"),
                    ExerciseSeed("Standing Calf Raise", 4, "15-20")
                )
            )
        )

        val templateDao = database.workoutTemplateDao()
        val exerciseTemplateDao = database.workoutTemplateExerciseDao()
        val exerciseDao = database.exerciseDao()

        for (seed in templates) {
            val templateId = templateDao.insert(
                WorkoutTemplateEntity(
                    name = seed.name,
                    description = seed.description,
                    isCustom = false
                )
            )
            seed.exercises.forEachIndexed { sortOrder, ex ->
                val exerciseEntity = exerciseDao.searchImmediate(ex.name) ?: return@forEachIndexed
                exerciseTemplateDao.insert(
                    WorkoutTemplateExerciseEntity(
                        templateId = templateId,
                        exerciseId = exerciseEntity.id,
                        targetSets = ex.sets,
                        targetReps = ex.reps,
                        targetWeightKg = ex.weightKg,
                        sortOrder = sortOrder
                    )
                )
            }
        }
    }

    companion object {
        const val WORK_NAME = "wger_seed"
        private const val MAX_RETRIES = 3
    }
}
