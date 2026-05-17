package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.reps.app.core.data.entity.GroceryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryItemDao {

    @Query("SELECT * FROM grocery_items WHERE listId = :listId ORDER BY category, addedAt")
    fun getItems(listId: Long): Flow<List<GroceryItemEntity>>

    @Insert
    suspend fun insert(entity: GroceryItemEntity): Long

    @Query("UPDATE grocery_items SET isBought = NOT isBought WHERE id = :id")
    suspend fun toggleBought(id: Long)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun delete(id: Long)
}
