package com.reps.app.core.domain.model

data class FoodItem(
    val id: Long = 0L,
    val name: String,
    val nameLocal: String? = null,
    val cuisineTags: List<String> = emptyList(),
    val servingDescription: String,
    val servingGrams: Double,
    val caloriesPerServing: Double,
    val proteinPerServing: Double,
    val carbsPerServing: Double,
    val fatPerServing: Double,
    val fiberPerServing: Double,
    val source: String,
    val externalId: String? = null,
    val isCustom: Boolean = false
) {
    companion object {
        const val SOURCE_ASSET = "ASSET"
        const val SOURCE_USDA = "USDA"
        const val SOURCE_OFF = "OFF"
        const val SOURCE_NINJAS = "NINJAS"
        const val SOURCE_CUSTOM = "CUSTOM"
    }
}
