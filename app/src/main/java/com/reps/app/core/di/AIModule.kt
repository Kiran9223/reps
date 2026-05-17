package com.reps.app.core.di

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.reps.app.BuildConfig
import com.reps.app.ai.AIRepository
import com.reps.app.ai.GeminiRepository
import com.reps.app.ai.HybridAIRepository
import com.reps.app.ai.MediaPipeAIRepository
import com.reps.app.ai.RuleBasedAIRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val MODEL_PATH = "/data/local/tmp/llm/model.task"

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideAIRepository(@ApplicationContext context: Context): AIRepository {
        val onDevice: AIRepository = try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(MODEL_PATH)
                .setMaxTokens(1024)
                .build()
            MediaPipeAIRepository(LlmInference.createFromOptions(context, options))
        } catch (e: Exception) {
            RuleBasedAIRepository()
        }

        val geminiKey = BuildConfig.GEMINI_API_KEY
        return if (geminiKey.isNotBlank()) {
            HybridAIRepository(onDevice = onDevice, gemini = GeminiRepository(geminiKey))
        } else {
            onDevice
        }
    }
}
