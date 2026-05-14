package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.reps.app.core.data.entity.MealLogEntity
import com.reps.app.core.data.relation.MealLogWithEntries
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MealLogEntity): Long

    @Update
    suspend fun update(log: MealLogEntity)

    @Query("SELECT * FROM meal_logs WHERE date = :date ORDER BY mealType ASC")
    fun getByDate(date: Long): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_logs WHERE id = :id")
    suspend fun getById(id: Long): MealLogEntity?

    @Query("SELECT * FROM meal_logs WHERE date = :date AND mealType = :mealType LIMIT 1")
    suspend fun getByDateAndType(date: Long, mealType: String): MealLogEntity?

    @Query("DELETE FROM meal_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("SELECT * FROM meal_logs WHERE date = :date")
    fun getWithEntriesForDate(date: Long): Flow<List<MealLogWithEntries>>

    @Transaction
    @Query("SELECT * FROM meal_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getWithEntriesForDateRange(startDate: Long, endDate: Long): Flow<List<MealLogWithEntries>>
}
