package com.reps.app.core.data.repository

import com.reps.app.BuildConfig
import com.reps.app.core.data.dao.FoodItemDao
import com.reps.app.core.data.mapper.toFoodItemEntity
import com.reps.app.core.data.mapper.toDomainModel
import com.reps.app.core.data.mapper.toEntity
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.model.FoodItem
import com.reps.app.core.domain.repository.FoodRepository
import com.reps.app.core.network.api.ApiNinjasApiService
import com.reps.app.core.network.api.OpenFoodFactsApiService
import com.reps.app.core.network.api.UsdaApiService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val foodItemDao: FoodItemDao,
    private val usdaApiService: UsdaApiService,
    private val offApiService: OpenFoodFactsApiService,
    private val apiNinjasService: ApiNinjasApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FoodRepository {

    override fun searchFoods(query: String): Flow<List<FoodItem>> = flow {
        val trimmed = query.trim()
        val local = foodItemDao.searchImmediate(trimmed)
        emit(local.map { it.toDomainModel() })

        if (trimmed.length >= 3 && local.size < 10) {
            // Use the first significant word to re-query Room after insert,
            // since API responses may name items differently than the query
            // (e.g. "whole grain bread" vs query "whole grains bread").
            val searchWord = trimmed.split("\\s+".toRegex())
                .firstOrNull { it.length >= 3 } ?: trimmed

            val usdaResults = runCatching {
                usdaApiService.searchFoods(
                    query = trimmed,
                    apiKey = BuildConfig.USDA_API_KEY,
                    pageSize = 25,
                    dataType = listOf("Branded", "Foundation", "SR Legacy")
                ).foods
            }.getOrDefault(emptyList())

            if (usdaResults.isNotEmpty()) {
                foodItemDao.insertAll(usdaResults.map { it.toFoodItemEntity() })
                emit(foodItemDao.searchImmediate(searchWord).map { it.toDomainModel() })
            } else {
                // USDA returned nothing — fall back to API Ninjas
                runCatching {
                    apiNinjasService.getNutrition(
                        apiKey = BuildConfig.API_NINJAS_KEY,
                        query = trimmed
                    )
                }.getOrNull()?.let { ninjasFoods ->
                    if (ninjasFoods.isNotEmpty()) {
                        foodItemDao.insertAll(ninjasFoods.map { it.toFoodItemEntity() })
                        emit(foodItemDao.searchImmediate(searchWord).map { it.toDomainModel() })
                    }
                }
            }
        }
    }.flowOn(ioDispatcher)

    override fun getFoodById(id: Long): Flow<FoodItem?> = flow {
        emit(foodItemDao.getById(id)?.toDomainModel())
    }.flowOn(ioDispatcher)

    override fun getFoodByBarcode(barcode: String): Flow<FoodItem?> = flow {
        val local = foodItemDao.getByExternalId(barcode, FoodItem.SOURCE_OFF)
        if (local != null) {
            emit(local.toDomainModel())
            return@flow
        }

        emit(null) // signals loading state to caller

        runCatching {
            val response = offApiService.getProductByBarcode(barcode)
            if (response.status == 1) {
                val entity = response.toFoodItemEntity(barcode)
                if (entity != null) {
                    val id = foodItemDao.insert(entity)
                    emit(foodItemDao.getById(id)?.toDomainModel())
                }
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun createCustomFood(food: FoodItem): Long =
        withContext(ioDispatcher) {
            foodItemDao.insert(food.toEntity())
        }

    override fun getRecentFoods(): Flow<List<FoodItem>> =
        foodItemDao.getRecent(30)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    override fun getFoodsByCuisineTag(tag: String): Flow<List<FoodItem>> =
        foodItemDao.getByCuisineTag(tag)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    override fun getCustomFoods(): Flow<List<FoodItem>> =
        foodItemDao.getCustom()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    override suspend fun deleteCustomFood(id: Long) =
        withContext(ioDispatcher) { foodItemDao.softDelete(id) }
}
