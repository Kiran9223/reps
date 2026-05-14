package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reps.app.core.data.entity.MealPlanTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: MealPlanTemplateEntity): Long

    @Update
    suspend fun update(template: MealPlanTemplateEntity)

    @Query("SELECT * FROM meal_plan_templates WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAll(): Flow<List<MealPlanTemplateEntity>>

    @Query("SELECT * FROM meal_plan_templates WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): MealPlanTemplateEntity?

    @Query("UPDATE meal_plan_templates SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}
