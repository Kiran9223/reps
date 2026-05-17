package com.reps.app.core.domain.repository

import com.reps.app.core.domain.model.GroceryCategory
import com.reps.app.core.domain.model.GroceryListSummary
import com.reps.app.core.domain.model.SavedGroceryItem
import kotlinx.coroutines.flow.Flow

interface GroceryRepository {
    fun getLists(): Flow<List<GroceryListSummary>>
    suspend fun getListName(listId: Long): String
    fun getItems(listId: Long): Flow<Map<GroceryCategory, List<SavedGroceryItem>>>
    suspend fun createList(name: String): Long
    suspend fun deleteList(listId: Long)
    suspend fun addItem(listId: Long, name: String, category: GroceryCategory, quantity: Double = 1.0)
    suspend fun toggleItem(itemId: Long)
    suspend fun deleteItem(itemId: Long)
}
