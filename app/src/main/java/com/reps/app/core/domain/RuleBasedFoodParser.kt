package com.reps.app.core.domain

import com.reps.app.core.data.dao.FoodItemDao
import com.reps.app.core.data.mapper.toDomainModel
import com.reps.app.core.domain.model.FoodItem
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedFoodEntry(
    val food: FoodItem,
    val quantity: Double
)

@Singleton
class RuleBasedFoodParser @Inject constructor(
    private val foodItemDao: FoodItemDao
) {
    private val numberWords = mapOf(
        "a" to 1.0, "an" to 1.0, "one" to 1.0, "two" to 2.0, "three" to 3.0,
        "four" to 4.0, "five" to 5.0, "six" to 6.0, "half" to 0.5, "quarter" to 0.25
    )
    private val connectors = setOf("and", "with", "plus", "&")

    suspend fun parse(input: String): List<ParsedFoodEntry> {
        val segments = input.split(Regex(",|\\band\\b", RegexOption.IGNORE_CASE))
        val result = mutableListOf<ParsedFoodEntry>()
        for (segment in segments) {
            val tokens = segment.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (tokens.isEmpty()) continue

            val (quantity, foodTokens) = extractQuantity(tokens)
            val foodQuery = foodTokens
                .filter { it.lowercase() !in connectors }
                .joinToString(" ")
                .trim()

            if (foodQuery.isBlank()) continue

            val matches = foodItemDao.searchImmediate(foodQuery)
            val best = matches.firstOrNull() ?: continue
            result.add(ParsedFoodEntry(food = best.toDomainModel(), quantity = quantity))
        }
        return result
    }

    suspend fun lookupByName(name: String, quantity: Double): ParsedFoodEntry? {
        val matches = foodItemDao.searchImmediate(name)
        val best = matches.firstOrNull() ?: return null
        return ParsedFoodEntry(food = best.toDomainModel(), quantity = quantity)
    }

    private fun extractQuantity(tokens: List<String>): Pair<Double, List<String>> {
        if (tokens.isEmpty()) return 1.0 to tokens
        val first = tokens.first().lowercase()
        val numeric = first.toDoubleOrNull() ?: numberWords[first]
        return if (numeric != null) numeric to tokens.drop(1)
        else 1.0 to tokens
    }
}
