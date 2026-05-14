package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reps.app.core.data.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WeightLogEntity): Long

    @Update
    suspend fun update(log: WeightLogEntity)

    @Query("SELECT * FROM weight_logs ORDER BY date DESC")
    fun getAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs ORDER BY date DESC LIMIT 1")
    suspend fun getMostRecent(): WeightLogEntity?

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun delete(id: Long)
}
