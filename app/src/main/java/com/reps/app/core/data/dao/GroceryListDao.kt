package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.reps.app.core.data.entity.GroceryListEntity
import kotlinx.coroutines.flow.Flow

data class GroceryListSummaryRow(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val totalItems: Int,
    val boughtCount: Int
)

@Dao
interface GroceryListDao {

    @Query("SELECT name FROM grocery_lists WHERE id = :id LIMIT 1")
    suspend fun getNameById(id: Long): String?

    @Query("""
        SELECT gl.id, gl.name, gl.createdAt,
               COUNT(gi.id) AS totalItems,
               COALESCE(SUM(CASE WHEN gi.isBought = 1 THEN 1 ELSE 0 END), 0) AS boughtCount
        FROM grocery_lists gl
        LEFT JOIN grocery_items gi ON gl.id = gi.listId
        GROUP BY gl.id
        ORDER BY gl.createdAt DESC
    """)
    fun getListSummaries(): Flow<List<GroceryListSummaryRow>>

    @Insert
    suspend fun insert(entity: GroceryListEntity): Long

    @Query("DELETE FROM grocery_lists WHERE id = :id")
    suspend fun delete(id: Long)
}
