package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reps.app.core.data.entity.WorkoutTemplateExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WorkoutTemplateExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WorkoutTemplateExerciseEntity>)

    @Update
    suspend fun update(entry: WorkoutTemplateExerciseEntity)

    @Query("SELECT * FROM workout_template_exercises WHERE templateId = :templateId ORDER BY sortOrder ASC")
    fun getByTemplateId(templateId: Long): Flow<List<WorkoutTemplateExerciseEntity>>

    @Query("DELETE FROM workout_template_exercises WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM workout_template_exercises WHERE templateId = :templateId")
    suspend fun deleteByTemplateId(templateId: Long)

    @Query("SELECT * FROM workout_template_exercises WHERE templateId = :templateId ORDER BY sortOrder ASC")
    suspend fun getByTemplateIdImmediate(templateId: Long): List<WorkoutTemplateExerciseEntity>
}
