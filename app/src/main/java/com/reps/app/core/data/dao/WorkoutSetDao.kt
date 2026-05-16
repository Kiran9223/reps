package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reps.app.core.data.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: WorkoutSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<WorkoutSetEntity>)

    @Update
    suspend fun update(set: WorkoutSetEntity)

    @Query("SELECT * FROM workout_sets WHERE workoutLogId = :workoutLogId ORDER BY exerciseId, setNumber ASC")
    fun getByWorkoutLogId(workoutLogId: Long): Flow<List<WorkoutSetEntity>>

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM workout_sets WHERE workoutLogId = :workoutLogId")
    suspend fun deleteByWorkoutLogId(workoutLogId: Long)

    @Query("SELECT COUNT(*) FROM workout_sets WHERE workoutLogId = :workoutLogId AND isCompleted = 1")
    suspend fun countCompleted(workoutLogId: Long): Int

    @Query("SELECT COUNT(DISTINCT exerciseId) FROM workout_sets WHERE workoutLogId = :workoutLogId")
    suspend fun countExercises(workoutLogId: Long): Int

    @Query("UPDATE workout_sets SET isCompleted = 1, reps = COALESCE(:reps, reps), weightKg = COALESCE(:weightKg, weightKg) WHERE id = :setId")
    suspend fun markCompleted(setId: Long, reps: Int? = null, weightKg: Double? = null)
}
