package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reps.app.core.data.entity.MealPlanSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanSlotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(slot: MealPlanSlotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<MealPlanSlotEntity>)

    @Update
    suspend fun update(slot: MealPlanSlotEntity)

    @Query("SELECT * FROM meal_plan_slots WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MealPlanSlotEntity?

    @Query("SELECT * FROM meal_plan_slots WHERE templateId = :templateId ORDER BY dayOfWeek, sortOrder ASC")
    fun getByTemplateId(templateId: Long): Flow<List<MealPlanSlotEntity>>

    @Query("SELECT * FROM meal_plan_slots WHERE templateId = :templateId AND dayOfWeek = :dayOfWeek AND mealType = :mealType")
    fun getByDayAndMeal(templateId: Long, dayOfWeek: Int, mealType: String): Flow<List<MealPlanSlotEntity>>

    @Query("DELETE FROM meal_plan_slots WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM meal_plan_slots WHERE templateId = :templateId")
    suspend fun deleteByTemplateId(templateId: Long)
}
