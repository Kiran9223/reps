package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reps.app.core.data.entity.WorkoutLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WorkoutLogEntity): Long

    @Update
    suspend fun update(log: WorkoutLogEntity)

    @Query("SELECT * FROM workout_logs WHERE date = :date ORDER BY createdAt DESC")
    fun getByDate(date: Long): Flow<List<WorkoutLogEntity>>

    @Query("SELECT * FROM workout_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutLogEntity>>

    @Query("SELECT * FROM workout_logs WHERE id = :id")
    suspend fun getById(id: Long): WorkoutLogEntity?

    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM workout_logs WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<WorkoutLogEntity?>

    @Query("SELECT * FROM workout_logs ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<WorkoutLogEntity>>
}
