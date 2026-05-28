package com.reps.app.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.reps.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIComponents @Inject constructor(
    @ApplicationContext context: Context,
    cloudAssistGate: CloudAssistGate
) {
    val privacyStatus: AIPrivacyStatus
    val repository: AIRepository

    init {
        val (onDevice, onDeviceLlmActive) = try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(OnDeviceModelConfig.MODEL_PATH)
                .setMaxTokens(1024)
                .build()
            MediaPipeAIRepository(LlmInference.createFromOptions(context, options)) to true
        } catch (_: Exception) {
            RuleBasedAIRepository() to false
        }

        privacyStatus = AIPrivacyStatus(
            onDeviceLlmActive = onDeviceLlmActive,
            cloudAssistAvailable = cloudAssistGate.hasCloudKey
        )
        repository = if (cloudAssistGate.hasCloudKey) {
            HybridAIRepository(
                onDevice = onDevice,
                gemini = GeminiRepository(BuildConfig.GEMINI_API_KEY),
                cloudAssistGate = cloudAssistGate
            )
        } else {
            onDevice
        }
    }
}
