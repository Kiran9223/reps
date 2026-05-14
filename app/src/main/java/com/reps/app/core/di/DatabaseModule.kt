package com.reps.app.core.di

import android.content.Context
import androidx.room.Room
import com.reps.app.core.data.RepsDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRepsDatabase(@ApplicationContext context: Context): RepsDatabase =
        Room.databaseBuilder(context, RepsDatabase::class.java, "reps.db").build()

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
}
