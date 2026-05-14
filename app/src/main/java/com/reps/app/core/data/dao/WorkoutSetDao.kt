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
}
