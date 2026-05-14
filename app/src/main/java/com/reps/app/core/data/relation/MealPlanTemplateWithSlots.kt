package com.reps.app.core.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.reps.app.core.data.entity.FoodItemEntity
import com.reps.app.core.data.entity.MealPlanSlotEntity
import com.reps.app.core.data.entity.MealPlanTemplateEntity

data class MealPlanSlotWithFood(
    @Embedded val slot: MealPlanSlotEntity,
    @Relation(parentColumn = "foodItemId", entityColumn = "id")
    val food: FoodItemEntity
)

data class MealPlanTemplateWithSlots(
    @Embedded val template: MealPlanTemplateEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId",
        entity = MealPlanSlotEntity::class
    )
    val slots: List<MealPlanSlotWithFood>
)
