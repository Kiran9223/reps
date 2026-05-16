package com.reps.app.core.di

import com.reps.app.BuildConfig
import com.reps.app.ai.AIRepository
import com.reps.app.ai.GeminiRepository
import com.reps.app.ai.RuleBasedAIRepository
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
    fun provideAIRepository(): AIRepository {
        val geminiKey = BuildConfig.GEMINI_API_KEY
        return if (geminiKey.isNotBlank()) {
            GeminiRepository(geminiKey)
        } else {
            RuleBasedAIRepository()
        }
    }
}
