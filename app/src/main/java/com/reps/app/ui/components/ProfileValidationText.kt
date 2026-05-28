package com.reps.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.reps.app.R
import com.reps.app.core.domain.ProfileValidationError

@Composable
fun profileValidationErrorMessage(error: ProfileValidationError): String = when (error) {
    ProfileValidationError.NAME_REQUIRED -> stringResource(R.string.profile_error_name)
    ProfileValidationError.AGE_INVALID -> stringResource(R.string.profile_error_age)
    ProfileValidationError.WEIGHT_INVALID -> stringResource(R.string.profile_error_weight)
    ProfileValidationError.TARGET_WEIGHT_INVALID -> stringResource(R.string.profile_error_target_weight)
    ProfileValidationError.HEIGHT_INVALID -> stringResource(R.string.profile_error_height)
}
