package com.reps.app.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.reps.app.core.data.RepsDatabase
import com.reps.app.core.data.dao.AIChatMessageDao
import com.reps.app.core.data.dao.BodyMeasurementDao
import com.reps.app.core.data.dao.ExerciseDao
import com.reps.app.core.data.dao.FoodItemDao
import com.reps.app.core.data.dao.GroceryItemDao
import com.reps.app.core.data.dao.GroceryListDao
import com.reps.app.core.data.dao.MealLogDao
import com.reps.app.core.data.dao.MealLogEntryDao
import com.reps.app.core.data.dao.MealPlanSlotDao
import com.reps.app.core.data.dao.MealPlanTemplateDao
import com.reps.app.core.data.dao.WeightLogDao
import com.reps.app.core.data.dao.WorkoutLogDao
import com.reps.app.core.data.dao.WorkoutSetDao
import com.reps.app.core.data.dao.WorkoutTemplateDao
import com.reps.app.core.data.dao.WorkoutTemplateExerciseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `grocery_lists` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `grocery_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `listId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `quantity` REAL NOT NULL DEFAULT 1.0,
                `isBought` INTEGER NOT NULL DEFAULT 0,
                `addedAt` INTEGER NOT NULL,
                FOREIGN KEY(`listId`) REFERENCES `grocery_lists`(`id`) ON DELETE CASCADE
            )"""
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_grocery_items_listId` ON `grocery_items` (`listId`)"
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `ai_chat_messages` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `content` TEXT NOT NULL,
                `isFromUser` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ai_chat_messages_timestamp` ON `ai_chat_messages` (`timestamp`)"
        )
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `body_measurements` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` INTEGER NOT NULL,
                `waistCm` REAL,
                `chestCm` REAL,
                `armsCm` REAL,
                `thighsCm` REAL,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_body_measurements_date` ON `body_measurements` (`date`)"
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRepsDatabase(@ApplicationContext context: Context): RepsDatabase =
        Room.databaseBuilder(context, RepsDatabase::class.java, "reps.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides fun provideFoodItemDao(db: RepsDatabase): FoodItemDao = db.foodItemDao()
    @Provides fun provideMealLogDao(db: RepsDatabase): MealLogDao = db.mealLogDao()
    @Provides fun provideMealLogEntryDao(db: RepsDatabase): MealLogEntryDao = db.mealLogEntryDao()
    @Provides fun provideExerciseDao(db: RepsDatabase): ExerciseDao = db.exerciseDao()
    @Provides fun provideWorkoutTemplateDao(db: RepsDatabase): WorkoutTemplateDao = db.workoutTemplateDao()
    @Provides fun provideWorkoutTemplateExerciseDao(db: RepsDatabase): WorkoutTemplateExerciseDao = db.workoutTemplateExerciseDao()
    @Provides fun provideWorkoutLogDao(db: RepsDatabase): WorkoutLogDao = db.workoutLogDao()
    @Provides fun provideWorkoutSetDao(db: RepsDatabase): WorkoutSetDao = db.workoutSetDao()
    @Provides fun provideWeightLogDao(db: RepsDatabase): WeightLogDao = db.weightLogDao()
    @Provides fun provideMealPlanTemplateDao(db: RepsDatabase): MealPlanTemplateDao = db.mealPlanTemplateDao()
    @Provides fun provideMealPlanSlotDao(db: RepsDatabase): MealPlanSlotDao = db.mealPlanSlotDao()
    @Provides fun provideBodyMeasurementDao(db: RepsDatabase): BodyMeasurementDao = db.bodyMeasurementDao()
    @Provides fun provideAIChatMessageDao(db: RepsDatabase): AIChatMessageDao = db.aiChatMessageDao()
    @Provides fun provideGroceryListDao(db: RepsDatabase): GroceryListDao = db.groceryListDao()
    @Provides fun provideGroceryItemDao(db: RepsDatabase): GroceryItemDao = db.groceryItemDao()
}
