package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reps.app.core.data.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: WorkoutTemplateEntity): Long

    @Update
    suspend fun update(template: WorkoutTemplateEntity)

    @Query("SELECT * FROM workout_templates WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAll(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): WorkoutTemplateEntity?

    @Query("UPDATE workout_templates SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}
