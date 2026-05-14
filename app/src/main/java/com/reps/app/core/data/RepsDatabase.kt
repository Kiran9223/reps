package com.reps.app.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.reps.app.core.data.converter.Converters
import com.reps.app.core.data.dao.ExerciseDao
import com.reps.app.core.data.dao.FoodItemDao
import com.reps.app.core.data.dao.MealLogDao
import com.reps.app.core.data.dao.MealLogEntryDao
import com.reps.app.core.data.dao.MealPlanSlotDao
import com.reps.app.core.data.dao.MealPlanTemplateDao
import com.reps.app.core.data.dao.WeightLogDao
import com.reps.app.core.data.dao.WorkoutLogDao
import com.reps.app.core.data.dao.WorkoutSetDao
import com.reps.app.core.data.dao.WorkoutTemplateDao
import com.reps.app.core.data.dao.WorkoutTemplateExerciseDao
import com.reps.app.core.data.entity.ExerciseEntity
import com.reps.app.core.data.entity.FoodItemEntity
import com.reps.app.core.data.entity.MealLogEntity
import com.reps.app.core.data.entity.MealLogEntryEntity
import com.reps.app.core.data.entity.MealPlanSlotEntity
import com.reps.app.core.data.entity.MealPlanTemplateEntity
import com.reps.app.core.data.entity.WeightLogEntity
import com.reps.app.core.data.entity.WorkoutLogEntity
import com.reps.app.core.data.entity.WorkoutSetEntity
import com.reps.app.core.data.entity.WorkoutTemplateEntity
import com.reps.app.core.data.entity.WorkoutTemplateExerciseEntity

@Database(
    entities = [
        FoodItemEntity::class,
        MealLogEntity::class,
        MealLogEntryEntity::class,
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutTemplateExerciseEntity::class,
        WorkoutLogEntity::class,
        WorkoutSetEntity::class,
        WeightLogEntity::class,
        MealPlanTemplateEntity::class,
        MealPlanSlotEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RepsDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun mealLogEntryDao(): MealLogEntryDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutTemplateExerciseDao(): WorkoutTemplateExerciseDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun mealPlanTemplateDao(): MealPlanTemplateDao
    abstract fun mealPlanSlotDao(): MealPlanSlotDao
}
