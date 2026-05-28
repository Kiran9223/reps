package com.reps.app.ai

/**
 * Build-time and runtime snapshot of where AI runs. Used for in-app privacy disclosure only.
 */
data class AIPrivacyStatus(
    /** MediaPipe Gemma model loaded at [MODEL_PATH]. */
    val onDeviceLlmActive: Boolean,
    /** [BuildConfig.GEMINI_API_KEY] is non-empty — cloud features can be enabled in Settings. */
    val cloudAssistAvailable: Boolean
)
