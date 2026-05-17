package com.reps.app.core.data.repository

import com.reps.app.core.data.dao.GroceryItemDao
import com.reps.app.core.data.dao.GroceryListDao
import com.reps.app.core.data.entity.GroceryItemEntity
import com.reps.app.core.data.entity.GroceryListEntity
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.model.GroceryCategory
import com.reps.app.core.domain.model.GroceryListSummary
import com.reps.app.core.domain.model.SavedGroceryItem
import com.reps.app.core.domain.repository.GroceryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroceryRepositoryImpl @Inject constructor(
    private val groceryListDao: GroceryListDao,
    private val groceryItemDao: GroceryItemDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : GroceryRepository {

    override fun getLists(): Flow<List<GroceryListSummary>> =
        groceryListDao.getListSummaries()
            .map { rows ->
                rows.map { row ->
                    GroceryListSummary(
                        id = row.id,
                        name = row.name,
                        totalItems = row.totalItems,
                        boughtCount = row.boughtCount,
                        createdAt = row.createdAt
                    )
                }
            }
            .flowOn(ioDispatcher)

    override suspend fun getListName(listId: Long): String = withContext(ioDispatcher) {
        groceryListDao.getNameById(listId) ?: ""
    }

    override fun getItems(listId: Long): Flow<Map<GroceryCategory, List<SavedGroceryItem>>> =
        groceryItemDao.getItems(listId)
            .map { entities ->
                entities
                    .map { it.toDomain() }
                    .groupBy { it.category }
                    .let { map ->
                        GroceryCategory.entries.associateWith { cat ->
                            map[cat] ?: emptyList()
                        }.filterValues { it.isNotEmpty() }
                    }
            }
            .flowOn(ioDispatcher)

    override suspend fun createList(name: String): Long = withContext(ioDispatcher) {
        groceryListDao.insert(GroceryListEntity(name = name))
    }

    override suspend fun deleteList(listId: Long) = withContext(ioDispatcher) {
        groceryListDao.delete(listId)
    }

    override suspend fun addItem(
        listId: Long,
        name: String,
        category: GroceryCategory,
        quantity: Double
    ) = withContext(ioDispatcher) {
        groceryItemDao.insert(
            GroceryItemEntity(
                listId = listId,
                name = name,
                category = category.name,
                quantity = quantity
            )
        )
        Unit
    }

    override suspend fun toggleItem(itemId: Long) = withContext(ioDispatcher) {
        groceryItemDao.toggleBought(itemId)
    }

    override suspend fun deleteItem(itemId: Long) = withContext(ioDispatcher) {
        groceryItemDao.delete(itemId)
    }

    private fun GroceryItemEntity.toDomain() = SavedGroceryItem(
        id = id,
        listId = listId,
        name = name,
        category = runCatching { GroceryCategory.valueOf(category) }.getOrElse { GroceryCategory.PANTRY },
        quantity = quantity,
        isBought = isBought
    )
}
