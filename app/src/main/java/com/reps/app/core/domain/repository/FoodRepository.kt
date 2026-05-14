package com.reps.app.core.domain.repository

import com.reps.app.core.domain.model.FoodItem
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    fun searchFoods(query: String): Flow<List<FoodItem>>
    fun getFoodById(id: Long): Flow<FoodItem?>
    fun getFoodByBarcode(barcode: String): Flow<FoodItem?>
    suspend fun createCustomFood(food: FoodItem): Long
    fun getRecentFoods(): Flow<List<FoodItem>>
    fun getFoodsByCuisineTag(tag: String): Flow<List<FoodItem>>
    fun getCustomFoods(): Flow<List<FoodItem>>
}
