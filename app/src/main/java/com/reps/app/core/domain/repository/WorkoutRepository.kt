package com.reps.app.core.domain.repository

import com.reps.app.core.domain.model.Exercise
import com.reps.app.core.domain.model.ExerciseFilter
import com.reps.app.core.domain.model.TemplateExerciseDraft
import com.reps.app.core.domain.model.WorkoutSession
import com.reps.app.core.domain.model.WorkoutSummary
import com.reps.app.core.domain.model.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getExercises(filter: ExerciseFilter): Flow<List<Exercise>>
    fun getTemplates(): Flow<List<WorkoutTemplate>>
    fun getSession(workoutLogId: Long): Flow<WorkoutSession?>
    fun getWorkoutHistory(limit: Int): Flow<List<WorkoutSummary>>
    suspend fun startWorkout(templateId: Long?): Long
    suspend fun startQuickWorkout(name: String, exercises: List<TemplateExerciseDraft>): Long
    suspend fun logSet(workoutLogId: Long, exerciseId: Long, setNumber: Int, reps: Int?, weightKg: Double?): Long
    suspend fun completeSet(setId: Long, reps: Int?, weightKg: Double?)
    suspend fun completeWorkout(workoutLogId: Long)
    suspend fun getExerciseById(id: Long): Exercise?
    suspend fun createTemplate(name: String, description: String?, exercises: List<TemplateExerciseDraft>): Long
    suspend fun updateTemplate(templateId: Long, name: String, description: String?, exercises: List<TemplateExerciseDraft>)
    suspend fun deleteTemplate(templateId: Long)
    suspend fun getPersonalRecord(exerciseId: Long): Double?
    suspend fun searchAndImportFromWger(query: String): Result<Int>
}
