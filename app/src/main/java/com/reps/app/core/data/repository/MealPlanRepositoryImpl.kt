package com.reps.app.core.data.repository

import com.reps.app.core.data.dao.FoodItemDao
import com.reps.app.core.data.dao.MealPlanSlotDao
import com.reps.app.core.data.dao.MealPlanTemplateDao
import com.reps.app.core.data.datastore.AppSettingsDataStore
import com.reps.app.core.data.entity.MealPlanSlotEntity
import com.reps.app.core.data.entity.MealPlanTemplateEntity
import com.reps.app.core.data.mapper.toDomain
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.model.MealPlanTemplate
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlanRepositoryImpl @Inject constructor(
    private val templateDao: MealPlanTemplateDao,
    private val slotDao: MealPlanSlotDao,
    private val foodItemDao: FoodItemDao,
    private val appSettingsDataStore: AppSettingsDataStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MealPlanRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getActiveMealPlan(): Flow<MealPlanTemplate?> =
        appSettingsDataStore.activeMealPlanId
            .flatMapLatest { id ->
                if (id == null) flowOf(null)
                else templateDao.getByIdWithSlots(id).map { it?.toDomain() }
            }
            .flowOn(ioDispatcher)

    override fun getAllTemplates(): Flow<List<MealPlanTemplate>> =
        templateDao.getAllWithSlots()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun setActivePlan(templateId: Long) {
        withContext(ioDispatcher) {
            appSettingsDataStore.setActiveMealPlanId(templateId)
        }
    }

    override suspend fun cloneTemplate(templateId: Long): Long = withContext(ioDispatcher) {
        val original = templateDao.getByIdWithSlotsImmediate(templateId)
            ?: error("Template $templateId not found")
        val newTemplate = original.template.copy(
            id = 0,
            name = "${original.template.name} (Copy)",
            isCustom = true,
            createdAt = System.currentTimeMillis()
        )
        val newId = templateDao.insert(newTemplate)
        val newSlots = original.slots.map { swf ->
            swf.slot.copy(id = 0, templateId = newId)
        }
        slotDao.insertAll(newSlots)
        newId
    }

    override suspend fun updateSlotFood(slotId: Long, foodItemId: Long, servingMultiplier: Double) {
        withContext(ioDispatcher) {
            val slot = slotDao.getById(slotId) ?: return@withContext
            slotDao.update(slot.copy(foodItemId = foodItemId, servingMultiplier = servingMultiplier))
        }
    }

    override suspend fun createPlan(
        name: String,
        description: String?,
        cuisineType: String,
        draftSlots: Map<Int, Map<MealSlot, List<Pair<Long, Double>>>>
    ): Long = withContext(ioDispatcher) {
        val template = MealPlanTemplateEntity(
            name = name,
            description = description,
            cuisineType = cuisineType,
            daysPerWeek = 4,
            mealsPerDay = 4,
            isCustom = true
        )
        val templateId = templateDao.insert(template)
        insertDraftSlots(templateId, draftSlots)
        templateId
    }

    override suspend fun updatePlan(
        templateId: Long,
        name: String,
        description: String?,
        cuisineType: String,
        draftSlots: Map<Int, Map<MealSlot, List<Pair<Long, Double>>>>
    ) = withContext(ioDispatcher) {
        val existing = templateDao.getById(templateId) ?: return@withContext
        templateDao.update(existing.copy(name = name, description = description, cuisineType = cuisineType))
        slotDao.deleteByTemplateId(templateId)
        insertDraftSlots(templateId, draftSlots)
    }

    override suspend fun deletePlan(templateId: Long) = withContext(ioDispatcher) {
        val currentActive = appSettingsDataStore.activeMealPlanId.first()
        templateDao.softDelete(templateId)
        if (currentActive == templateId) {
            val next = templateDao.getFirstAvailable()
            if (next != null) appSettingsDataStore.setActiveMealPlanId(next.id)
            else appSettingsDataStore.clearActiveMealPlanId()
        }
    }

    private suspend fun insertDraftSlots(
        templateId: Long,
        draftSlots: Map<Int, Map<MealSlot, List<Pair<Long, Double>>>>
    ) {
        var order = 0
        val slots = mutableListOf<MealPlanSlotEntity>()
        draftSlots.forEach { (dayIndex, slotMap) ->
            slotMap.forEach { (mealSlot, foods) ->
                foods.forEach { (foodId, servings) ->
                    slots += MealPlanSlotEntity(
                        templateId = templateId,
                        dayOfWeek = dayIndex,
                        mealType = mealSlot.name,
                        foodItemId = foodId,
                        servingMultiplier = servings,
                        sortOrder = order++
                    )
                }
            }
        }
        if (slots.isNotEmpty()) slotDao.insertAll(slots)
    }
}
