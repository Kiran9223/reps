package com.reps.app.core.domain.model

data class SavedGroceryItem(
    val id: Long,
    val listId: Long,
    val name: String,
    val category: GroceryCategory,
    val quantity: Double,
    val isBought: Boolean
)
