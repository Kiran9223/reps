package com.reps.app.core.domain.model

data class GroceryListSummary(
    val id: Long,
    val name: String,
    val totalItems: Int,
    val boughtCount: Int,
    val createdAt: Long
)
