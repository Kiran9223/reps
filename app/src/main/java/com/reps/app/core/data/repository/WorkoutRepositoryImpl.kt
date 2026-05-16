package com.reps.app.core.data.repository

import com.reps.app.core.data.RepsDatabase
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.data.entity.WorkoutLogEntity
import com.reps.app.core.data.entity.WorkoutSetEntity
import com.reps.app.core.data.mapper.toDomain
import com.reps.app.core.domain.model.CompletedSet
import com.reps.app.core.domain.model.Exercise
import com.reps.app.core.domain.model.ExerciseFilter
import com.reps.app.core.domain.model.SessionExercise
import com.reps.app.core.data.entity.WorkoutTemplateEntity
import com.reps.app.core.data.entity.WorkoutTemplateExerciseEntity
import com.reps.app.core.domain.model.TemplateExercise
import com.reps.app.core.domain.model.TemplateExerciseDraft
import com.reps.app.core.domain.model.WorkoutSession
import com.reps.app.core.domain.model.WorkoutSummary
import com.reps.app.core.domain.model.WorkoutTemplate
import com.reps.app.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val database: RepsDatabase,
    private val appSettingsDataStore: AppSettingsDataStore
) : WorkoutRepository {

    override fun getExercises(filter: ExerciseFilter): Flow<List<Exercise>> {
        val baseFlow = if (filter.shoulderSafeOnly) {
            database.exerciseDao().getShoulderSafe()
        } else {
            database.exerciseDao().getAll()
        }
        return baseFlow.map { entities ->
            entities
                .filter { e ->
                    val matchesQuery = filter.query.isBlank() ||
                        e.name.contains(filter.query, ignoreCase = true)
                    val matchesMuscle = filter.muscleGroup == null ||
                        e.muscleGroups.any { it.contains(filter.muscleGroup, ignoreCase = true) }
                    matchesQuery && matchesMuscle
                }
                .map { it.toDomain() }
        }
    }

    override fun getTemplates(): Flow<List<WorkoutTemplate>> =
        database.workoutTemplateDao().getAllWithExercises().map { list ->
            list.filter { !it.template.isDeleted }.map { twe ->
                WorkoutTemplate(
                    id = twe.template.id,
                    name = twe.template.name,
                    description = twe.template.description,
                    isCustom = twe.template.isCustom,
                    exercises = twe.templateExercises
                        .sortedBy { it.templateExercise.sortOrder }
                        .map { twee ->
                            TemplateExercise(
                                id = twee.templateExercise.id,
                                exercise = twee.exercise.toDomain(),
                                targetSets = twee.templateExercise.targetSets,
                                targetReps = twee.templateExercise.targetReps,
                                targetWeightKg = twee.templateExercise.targetWeightKg,
                                sortOrder = twee.templateExercise.sortOrder
                            )
                        }
                )
            }
        }

    override fun getSession(workoutLogId: Long): Flow<WorkoutSession?> {
        val logFlow = database.workoutLogDao().getByIdFlow(workoutLogId)
        val setsFlow = database.workoutSetDao().getByWorkoutLogId(workoutLogId)
        return combine(logFlow, setsFlow) { log, sets ->
            log ?: return@combine null
            val exerciseIds = sets.map { it.exerciseId }.distinct()
            val exercises = exerciseIds.mapNotNull { id ->
                database.exerciseDao().getById(id)?.toDomain()
            }
            val exerciseMap = exercises.associateBy { it.id }
            val sessionExercises = sets
                .groupBy { it.exerciseId }
                .mapNotNull { (exerciseId, exerciseSets) ->
                    val exercise = exerciseMap[exerciseId] ?: return@mapNotNull null
                    SessionExercise(
                        exercise = exercise,
                        sets = exerciseSets.sortedBy { it.setNumber }.map { s ->
                            CompletedSet(
                                id = s.id,
                                setNumber = s.setNumber,
                                reps = s.reps,
                                weightKg = s.weightKg,
                                isCompleted = s.isCompleted
                            )
                        }
                    )
                }
                .sortedBy { se -> exerciseIds.indexOf(se.exercise.id) }
            WorkoutSession(
                id = log.id,
                date = log.date,
                name = log.name,
                templateId = log.templateId,
                exercises = sessionExercises,
                durationMinutes = log.durationMinutes,
                notes = log.notes
            )
        }
    }

    override fun getWorkoutHistory(limit: Int): Flow<List<WorkoutSummary>> =
        database.workoutLogDao().getRecent(limit).map { logs ->
            logs.map { log ->
                val totalSets = database.workoutSetDao().countCompleted(log.id)
                val totalExercises = database.workoutSetDao().countExercises(log.id)
                WorkoutSummary(
                    id = log.id,
                    date = log.date,
                    name = log.name,
                    durationMinutes = log.durationMinutes,
                    totalSets = totalSets,
                    totalExercises = totalExercises
                )
            }
        }

    override suspend fun startWorkout(templateId: Long?): Long {
        val todayMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val template = templateId?.let { database.workoutTemplateDao().getWithExercisesById(it) }
        val name = template?.template?.name ?: "Quick Workout"
        val logId = database.workoutLogDao().insert(
            WorkoutLogEntity(
                date = todayMs,
                templateId = templateId,
                name = name
            )
        )
        if (template != null) {
            val entries = template.templateExercises.sortedBy { it.templateExercise.sortOrder }
            entries.forEachIndexed { _, twe ->
                repeat(twe.templateExercise.targetSets) { setIndex ->
                    database.workoutSetDao().insert(
                        WorkoutSetEntity(
                            workoutLogId = logId,
                            exerciseId = twe.templateExercise.exerciseId,
                            setNumber = setIndex + 1,
                            weightKg = twe.templateExercise.targetWeightKg
                        )
                    )
                }
            }
        }
        return logId
    }

    override suspend fun logSet(
        workoutLogId: Long,
        exerciseId: Long,
        setNumber: Int,
        reps: Int?,
        weightKg: Double?
    ): Long = database.workoutSetDao().insert(
        WorkoutSetEntity(
            workoutLogId = workoutLogId,
            exerciseId = exerciseId,
            setNumber = setNumber,
            reps = reps,
            weightKg = weightKg
        )
    )

    override suspend fun completeSet(setId: Long, reps: Int?, weightKg: Double?) {
        database.workoutSetDao().markCompleted(setId, reps, weightKg)
    }

    override suspend fun completeWorkout(workoutLogId: Long) {
        val log = database.workoutLogDao().getById(workoutLogId) ?: return
        val startMs = log.createdAt
        val durationMs = System.currentTimeMillis() - startMs
        val durationMinutes = (durationMs / 60_000).toInt().coerceAtLeast(1)
        database.workoutLogDao().update(log.copy(durationMinutes = durationMinutes))
    }

    override suspend fun getExerciseById(id: Long): Exercise? =
        database.exerciseDao().getById(id)?.toDomain()

    override suspend fun createTemplate(
        name: String,
        description: String?,
        exercises: List<TemplateExerciseDraft>
    ): Long {
        val templateId = database.workoutTemplateDao().insert(
            WorkoutTemplateEntity(name = name, description = description, isCustom = true)
        )
        insertDraftExercises(templateId, exercises)
        return templateId
    }

    override suspend fun updateTemplate(
        templateId: Long,
        name: String,
        description: String?,
        exercises: List<TemplateExerciseDraft>
    ) {
        val existing = database.workoutTemplateDao().getById(templateId) ?: return
        database.workoutTemplateDao().update(
            existing.copy(name = name, description = description)
        )
        database.workoutTemplateExerciseDao().deleteByTemplateId(templateId)
        insertDraftExercises(templateId, exercises)
    }

    override suspend fun deleteTemplate(templateId: Long) {
        database.workoutTemplateDao().softDelete(templateId)
    }

    private suspend fun insertDraftExercises(templateId: Long, exercises: List<TemplateExerciseDraft>) {
        val entities = exercises.mapIndexed { index, draft ->
            WorkoutTemplateExerciseEntity(
                templateId = templateId,
                exerciseId = draft.exerciseId,
                targetSets = draft.targetSets,
                targetReps = draft.targetReps,
                targetWeightKg = draft.targetWeightKg,
                sortOrder = index
            )
        }
        database.workoutTemplateExerciseDao().insertAll(entities)
    }
}
