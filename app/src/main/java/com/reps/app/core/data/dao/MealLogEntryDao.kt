package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reps.app.core.data.entity.MealLogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MealLogEntryEntity): Long

    @Update
    suspend fun update(entry: MealLogEntryEntity)

    @Query("SELECT * FROM meal_log_entries WHERE mealLogId = :mealLogId AND isDeleted = 0")
    fun getByMealLogId(mealLogId: Long): Flow<List<MealLogEntryEntity>>

    @Query("SELECT * FROM meal_log_entries WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): MealLogEntryEntity?

    @Query("UPDATE meal_log_entries SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("DELETE FROM meal_log_entries WHERE mealLogId = :mealLogId")
    suspend fun deleteByMealLogId(mealLogId: Long)
}
