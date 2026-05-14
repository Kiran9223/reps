package com.reps.app.core.domain.model

data class GroceryItem(
    val id: String,
    val name: String,
    val quantity: Double,
    val servingDescription: String,
    val category: GroceryCategory,
    val isBought: Boolean = false,
    val isCustom: Boolean = false
)
