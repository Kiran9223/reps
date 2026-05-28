package com.reps.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.reps.app.R
import com.reps.app.ai.AiUserMessages

@Composable
fun resolveAiErrorMessage(errorKey: String): String = when (errorKey) {
    AiUserMessages.PARSE_FAILED -> stringResource(R.string.ai_parse_error)
    AiUserMessages.ESTIMATE_FOOD_FAILED -> stringResource(R.string.ai_estimate_food_error)
    AiUserMessages.ESTIMATE_EXERCISE_FAILED -> stringResource(R.string.ai_estimate_exercise_error)
    AiUserMessages.GENERATE_WORKOUT_FAILED -> stringResource(R.string.ai_generate_workout_error)
    AiUserMessages.GROCERY_CATEGORIZE_FAILED -> stringResource(R.string.ai_grocery_categorize_error)
    AiUserMessages.SHOULDER_ALTERNATIVES_FAILED -> stringResource(R.string.ai_shoulder_alternatives_error)
    AiUserMessages.SAVE_EXERCISE_FAILED -> stringResource(R.string.custom_exercise_save_error)
    else -> stringResource(R.string.ai_error_generic)
}
