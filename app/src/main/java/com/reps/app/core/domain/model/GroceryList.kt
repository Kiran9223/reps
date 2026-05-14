package com.reps.app.core.domain.model

data class GroceryList(
    val templateId: Long,
    val templateName: String,
    val itemsByCategory: Map<GroceryCategory, List<GroceryItem>>
) {
    val allItems: List<GroceryItem>
        get() = GroceryCategory.entries.flatMap { itemsByCategory[it] ?: emptyList() }

    val totalItems: Int get() = allItems.size
    val boughtCount: Int get() = allItems.count { it.isBought }
}
