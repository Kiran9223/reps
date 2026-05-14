package com.reps.app.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reps.app.core.data.RepsDatabase
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.data.entity.MealPlanSlotEntity
import com.reps.app.core.data.entity.MealPlanTemplateEntity
import com.reps.app.core.data.mapper.toEntity
import com.reps.app.core.network.dto.FoodItemDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

@HiltWorker
class SeedDatabaseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: RepsDatabase,
    private val appSettingsDataStore: AppSettingsDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            seedFoodItems()
            seedMealPlanTemplates()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private suspend fun seedFoodItems() {
        if (appSettingsDataStore.isDbSeeded.first()) return
        val json = applicationContext.assets.open("indian_foods.json")
            .bufferedReader()
            .readText()
        val dtos = Json { ignoreUnknownKeys = true }
            .decodeFromString<List<FoodItemDto>>(json)
        database.foodItemDao().insertAll(dtos.map { it.toEntity() })
        appSettingsDataStore.setDbSeeded(true)
    }

    private suspend fun seedMealPlanTemplates() {
        if (appSettingsDataStore.isMealPlanSeeded.first()) return

        val templates = buildTemplateSeedData()
        val foodDao = database.foodItemDao()
        val templateDao = database.mealPlanTemplateDao()
        val slotDao = database.mealPlanSlotDao()

        var firstTemplateId: Long? = null

        for ((template, slotSeeds) in templates) {
            val templateId = templateDao.insert(template)
            if (firstTemplateId == null) firstTemplateId = templateId

            val slots = slotSeeds.mapNotNull { seed ->
                val food = foodDao.searchImmediate(seed.query).firstOrNull()
                    ?: return@mapNotNull null
                MealPlanSlotEntity(
                    templateId = templateId,
                    dayOfWeek = seed.dayOfWeek,
                    mealType = seed.mealType,
                    foodItemId = food.id,
                    servingMultiplier = seed.serving,
                    sortOrder = seed.sortOrder
                )
            }
            slotDao.insertAll(slots)
        }

        firstTemplateId?.let { appSettingsDataStore.setActiveMealPlanId(it) }
        appSettingsDataStore.setMealPlanSeeded(true)
    }

    private data class SlotSeed(
        val dayOfWeek: Int,
        val mealType: String,
        val query: String,
        val serving: Double = 1.0,
        val sortOrder: Int = 0
    )

    private fun buildTemplateSeedData(): List<Pair<MealPlanTemplateEntity, List<SlotSeed>>> {
        val southIndianCut = MealPlanTemplateEntity(
            name = "South Indian Cut Plan",
            description = "High-protein South Indian meals for fat loss",
            cuisineType = "South Indian",
            daysPerWeek = 4,
            mealsPerDay = 4
        ) to listOf(
            // Day 0 – Mon / Thu
            SlotSeed(0, "BREAKFAST", "Idli", 2.0, 0),
            SlotSeed(0, "BREAKFAST", "Sambar", 1.0, 1),
            SlotSeed(0, "LUNCH", "Brown Rice", 1.0, 0),
            SlotSeed(0, "LUNCH", "Dal", 1.0, 1),
            SlotSeed(0, "LUNCH", "Chicken", 1.0, 2),
            SlotSeed(0, "DINNER", "Roti", 2.0, 0),
            SlotSeed(0, "DINNER", "Dal", 1.0, 1),
            SlotSeed(0, "SNACKS", "Boiled Egg", 2.0, 0),
            // Day 1 – Tue / Fri
            SlotSeed(1, "BREAKFAST", "Oats", 1.0, 0),
            SlotSeed(1, "BREAKFAST", "Egg", 2.0, 1),
            SlotSeed(1, "LUNCH", "Rice", 1.0, 0),
            SlotSeed(1, "LUNCH", "Sambar", 1.0, 1),
            SlotSeed(1, "LUNCH", "Chicken", 1.0, 2),
            SlotSeed(1, "DINNER", "Chapati", 2.0, 0),
            SlotSeed(1, "DINNER", "Paneer", 1.0, 1),
            SlotSeed(1, "SNACKS", "Banana", 1.0, 0),
            // Day 2 – Wed / Sat
            SlotSeed(2, "BREAKFAST", "Dosa", 2.0, 0),
            SlotSeed(2, "LUNCH", "Rice", 1.0, 0),
            SlotSeed(2, "LUNCH", "Fish", 1.0, 1),
            SlotSeed(2, "DINNER", "Upma", 1.0, 0),
            SlotSeed(2, "DINNER", "Egg", 2.0, 1),
            SlotSeed(2, "SNACKS", "Curd", 1.0, 0),
            // Day 3 – Sunday
            SlotSeed(3, "BREAKFAST", "Poha", 1.0, 0),
            SlotSeed(3, "BREAKFAST", "Egg", 2.0, 1),
            SlotSeed(3, "LUNCH", "Biryani", 1.0, 0),
            SlotSeed(3, "DINNER", "Dal", 1.0, 0),
            SlotSeed(3, "DINNER", "Rice", 1.0, 1),
            SlotSeed(3, "SNACKS", "Peanut", 1.0, 0)
        )

        val highProtein = MealPlanTemplateEntity(
            name = "High Protein Plan",
            description = "Chicken, eggs and whey-heavy plan for muscle retention",
            cuisineType = "Mixed",
            daysPerWeek = 4,
            mealsPerDay = 4
        ) to listOf(
            SlotSeed(0, "BREAKFAST", "Egg", 3.0, 0),
            SlotSeed(0, "BREAKFAST", "Oats", 1.0, 1),
            SlotSeed(0, "LUNCH", "Chicken", 1.5, 0),
            SlotSeed(0, "LUNCH", "Rice", 1.0, 1),
            SlotSeed(0, "DINNER", "Chicken", 1.5, 0),
            SlotSeed(0, "DINNER", "Chapati", 2.0, 1),
            SlotSeed(0, "SNACKS", "Egg", 2.0, 0),
            SlotSeed(1, "BREAKFAST", "Egg", 3.0, 0),
            SlotSeed(1, "BREAKFAST", "Oats", 1.0, 1),
            SlotSeed(1, "LUNCH", "Chicken", 1.5, 0),
            SlotSeed(1, "LUNCH", "Brown Rice", 1.0, 1),
            SlotSeed(1, "DINNER", "Fish", 1.0, 0),
            SlotSeed(1, "DINNER", "Chapati", 2.0, 1),
            SlotSeed(1, "SNACKS", "Curd", 1.0, 0),
            SlotSeed(2, "BREAKFAST", "Egg", 3.0, 0),
            SlotSeed(2, "BREAKFAST", "Upma", 1.0, 1),
            SlotSeed(2, "LUNCH", "Chicken", 1.5, 0),
            SlotSeed(2, "LUNCH", "Dal", 1.0, 1),
            SlotSeed(2, "DINNER", "Chicken", 1.5, 0),
            SlotSeed(2, "DINNER", "Roti", 2.0, 1),
            SlotSeed(2, "SNACKS", "Paneer", 1.0, 0),
            SlotSeed(3, "BREAKFAST", "Egg", 3.0, 0),
            SlotSeed(3, "BREAKFAST", "Oats", 1.0, 1),
            SlotSeed(3, "LUNCH", "Chicken", 1.5, 0),
            SlotSeed(3, "LUNCH", "Rice", 1.0, 1),
            SlotSeed(3, "DINNER", "Fish", 1.0, 0),
            SlotSeed(3, "DINNER", "Chapati", 2.0, 1),
            SlotSeed(3, "SNACKS", "Curd", 1.0, 0)
        )

        val vegetarianSouthIndian = MealPlanTemplateEntity(
            name = "Vegetarian South Indian",
            description = "Soya, paneer and legume-heavy vegetarian plan",
            cuisineType = "South Indian",
            daysPerWeek = 4,
            mealsPerDay = 4
        ) to listOf(
            SlotSeed(0, "BREAKFAST", "Idli", 3.0, 0),
            SlotSeed(0, "BREAKFAST", "Sambar", 1.0, 1),
            SlotSeed(0, "LUNCH", "Rice", 1.0, 0),
            SlotSeed(0, "LUNCH", "Dal", 1.0, 1),
            SlotSeed(0, "LUNCH", "Paneer", 1.0, 2),
            SlotSeed(0, "DINNER", "Roti", 2.0, 0),
            SlotSeed(0, "DINNER", "Dal", 1.0, 1),
            SlotSeed(0, "SNACKS", "Curd", 1.0, 0),
            SlotSeed(1, "BREAKFAST", "Dosa", 2.0, 0),
            SlotSeed(1, "BREAKFAST", "Sambar", 1.0, 1),
            SlotSeed(1, "LUNCH", "Rice", 1.0, 0),
            SlotSeed(1, "LUNCH", "Soya", 1.0, 1),
            SlotSeed(1, "DINNER", "Chapati", 2.0, 0),
            SlotSeed(1, "DINNER", "Dal", 1.0, 1),
            SlotSeed(1, "SNACKS", "Banana", 1.0, 0),
            SlotSeed(2, "BREAKFAST", "Upma", 1.0, 0),
            SlotSeed(2, "BREAKFAST", "Curd", 1.0, 1),
            SlotSeed(2, "LUNCH", "Brown Rice", 1.0, 0),
            SlotSeed(2, "LUNCH", "Paneer", 1.0, 1),
            SlotSeed(2, "LUNCH", "Sambar", 1.0, 2),
            SlotSeed(2, "DINNER", "Roti", 2.0, 0),
            SlotSeed(2, "DINNER", "Soya", 1.0, 1),
            SlotSeed(2, "SNACKS", "Peanut", 1.0, 0),
            SlotSeed(3, "BREAKFAST", "Poha", 1.0, 0),
            SlotSeed(3, "BREAKFAST", "Curd", 1.0, 1),
            SlotSeed(3, "LUNCH", "Biryani", 1.0, 0),
            SlotSeed(3, "DINNER", "Dal", 1.0, 0),
            SlotSeed(3, "DINNER", "Rice", 1.0, 1),
            SlotSeed(3, "SNACKS", "Banana", 1.0, 0)
        )

        return listOf(southIndianCut, highProtein, vegetarianSouthIndian)
    }

    companion object {
        const val WORK_NAME = "seed_database"
        private const val MAX_RETRIES = 3
    }
}
