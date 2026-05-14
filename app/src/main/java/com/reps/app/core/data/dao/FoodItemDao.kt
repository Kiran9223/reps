package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reps.app.core.data.entity.FoodItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FoodItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FoodItemEntity>)

    @Update
    suspend fun update(item: FoodItemEntity)

    @Query("SELECT * FROM food_items WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAll(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): FoodItemEntity?

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY name ASC")
    fun search(query: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE cuisineTags LIKE '%' || :tag || '%' AND isDeleted = 0")
    fun getByCuisineTag(tag: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE source = :source AND isDeleted = 0")
    fun getBySource(source: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE isCustom = 1 AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getCustom(): Flow<List<FoodItemEntity>>

    @Query("UPDATE food_items SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT COUNT(*) FROM food_items WHERE source = 'ASSET'")
    suspend fun countSeededItems(): Int
}
