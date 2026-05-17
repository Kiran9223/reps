package com.reps.app.core.di

import com.reps.app.core.data.repository.FoodRepositoryImpl
import com.reps.app.core.data.repository.GroceryRepositoryImpl
import com.reps.app.core.data.repository.MealLogRepositoryImpl
import com.reps.app.core.data.repository.MealPlanRepositoryImpl
import com.reps.app.core.data.repository.ProgressRepositoryImpl
import com.reps.app.core.data.repository.WorkoutRepositoryImpl
import com.reps.app.core.domain.repository.FoodRepository
import com.reps.app.core.domain.repository.GroceryRepository
import com.reps.app.core.domain.repository.MealLogRepository
import com.reps.app.core.domain.repository.MealPlanRepository
import com.reps.app.core.domain.repository.ProgressRepository
import com.reps.app.core.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository

    @Binds @Singleton
    abstract fun bindMealLogRepository(impl: MealLogRepositoryImpl): MealLogRepository

    @Binds @Singleton
    abstract fun bindMealPlanRepository(impl: MealPlanRepositoryImpl): MealPlanRepository

    @Binds @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds @Singleton
    abstract fun bindGroceryRepository(impl: GroceryRepositoryImpl): GroceryRepository
}
