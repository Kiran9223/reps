package com.reps.app.core.data.repository

import com.reps.app.BuildConfig
import com.reps.app.core.data.dao.FoodItemDao
import com.reps.app.core.data.mapper.toFoodItemEntity
import com.reps.app.core.data.mapper.toDomainModel
import com.reps.app.core.data.mapper.toEntity
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.model.FoodItem
import com.reps.app.core.domain.repository.FoodRepository
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FoodRepository {

    override fun searchFoods(query: String): Flow<List<FoodItem>> = flow {
        val local = foodItemDao.searchImmediate(query)
        emit(local.map { it.toDomainModel() })

        if (query.length >= 2 && local.size < 3) {
            runCatching {
                val response = usdaApiService.searchFoods(
                    query = query,
                    apiKey = BuildConfig.USDA_API_KEY,
                    pageSize = 25,
                    dataType = "Foundation,SR Legacy"
                )
                val entities = response.foods.map { it.toFoodItemEntity() }
                if (entities.isNotEmpty()) {
                    foodItemDao.insertAll(entities)
                    val updated = foodItemDao.searchImmediate(query)
                    emit(updated.map { it.toDomainModel() })
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
}
