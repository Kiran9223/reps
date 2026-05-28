package com.reps.app.ai

import java.io.File

/** MediaPipe Gemma model location (developer push or future in-app install target). */
object OnDeviceModelConfig {
    const val MODEL_PATH = "/data/local/tmp/llm/model.task"

    fun isModelInstalled(): Boolean {
        val file = File(MODEL_PATH)
        return file.exists() && file.length() > 0L
    }
}
