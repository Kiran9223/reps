package com.reps.app.ai

/**
 * Maps throwables and repository failures to user-safe copy (no exception class names).
 */
object AiUserMessages {
    fun forThrowable(e: Throwable): String = when {
        e.message?.contains("quota", ignoreCase = true) == true ||
            e.message?.contains("limit: 0", ignoreCase = true) == true ->
            "ai_error_quota"
        e.message?.contains("rate", ignoreCase = true) == true ->
            "ai_error_rate_limit"
        e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
            e.message?.contains("network", ignoreCase = true) == true ||
            e.message?.contains("timeout", ignoreCase = true) == true ->
            "ai_error_network"
        else -> "ai_error_generic"
    }

    const val INSIGHT_FAILED = "ai_insight_error"
    const val SUGGESTION_FAILED = "ai_suggestion_error"
    const val PARSE_FAILED = "ai_parse_error"
    const val QUICK_WORKOUT_FAILED = "ai_quick_workout_error"
    const val ESTIMATE_FOOD_FAILED = "estimate_food_failed"
    const val ESTIMATE_EXERCISE_FAILED = "estimate_exercise_failed"
    const val GENERATE_WORKOUT_FAILED = "generate_workout_failed"
    const val SAVE_EXERCISE_FAILED = "save_exercise_failed"
    const val GROCERY_CATEGORIZE_FAILED = "grocery_categorize_failed"
    const val SHOULDER_ALTERNATIVES_FAILED = "shoulder_alternatives_failed"
}
