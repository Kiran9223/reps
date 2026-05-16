package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reps.app.core.data.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercises WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE isShoulderSafe = 1 AND isDeleted = 0 ORDER BY name ASC")
    fun getShoulderSafe(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE muscleGroups LIKE '%' || :muscleGroup || '%' AND isDeleted = 0")
    fun getByMuscleGroup(muscleGroup: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY name ASC")
    fun search(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("UPDATE exercises SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT COUNT(*) FROM exercises WHERE isDeleted = 0")
    suspend fun count(): Int

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY name ASC LIMIT 1")
    suspend fun searchImmediate(query: String): ExerciseEntity?
}
