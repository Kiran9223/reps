package com.reps.app.core.di

import com.reps.app.ai.AIComponents
import com.reps.app.ai.AIPrivacyStatus
import com.reps.app.ai.AIRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideAIRepository(components: AIComponents): AIRepository = components.repository

    @Provides
    @Singleton
    fun provideAIPrivacyStatus(components: AIComponents): AIPrivacyStatus = components.privacyStatus
}
