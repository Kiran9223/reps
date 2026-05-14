package com.reps.app.core.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.reps.app.core.data.entity.FoodItemEntity
import com.reps.app.core.data.entity.MealLogEntity
import com.reps.app.core.data.entity.MealLogEntryEntity

data class MealLogEntryWithFood(
    @Embedded val entry: MealLogEntryEntity,
    @Relation(parentColumn = "foodItemId", entityColumn = "id")
    val food: FoodItemEntity
)

data class MealLogWithEntries(
    @Embedded val log: MealLogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealLogId",
        entity = MealLogEntryEntity::class
    )
    val entries: List<MealLogEntryWithFood>
)
