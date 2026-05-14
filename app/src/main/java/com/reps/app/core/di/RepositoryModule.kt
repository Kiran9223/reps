package com.reps.app.core.di

import com.reps.app.core.data.repository.FoodRepositoryImpl
import com.reps.app.core.data.repository.MealLogRepositoryImpl
import com.reps.app.core.data.repository.MealPlanRepositoryImpl
import com.reps.app.core.domain.repository.FoodRepository
import com.reps.app.core.domain.repository.MealLogRepository
import com.reps.app.core.domain.repository.MealPlanRepository
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
}
