package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reps.app.core.data.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: BodyMeasurementEntity): Long

    @Query("SELECT * FROM body_measurements ORDER BY date DESC")
    fun getAll(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<BodyMeasurementEntity>>

    @Query("DELETE FROM body_measurements WHERE id = :id")
    suspend fun delete(id: Long)
}
