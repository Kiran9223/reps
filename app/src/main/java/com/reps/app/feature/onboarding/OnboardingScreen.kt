package com.reps.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.ui.theme.RepsTheme

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isCompleting) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.currentStep > 1) {
            LinearProgressIndicator(
                progress = { state.currentStep.toFloat() / state.totalSteps },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            if (state.currentStep > 1) {
                Text(
                    text = stringResource(R.string.onboarding_step_indicator, state.currentStep),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
            }

            when (state.currentStep) {
                1 -> WelcomeStep(onGetStarted = viewModel::nextStep)
                2 -> NameAgeStep(state = state, viewModel = viewModel)
                3 -> BodyStatsStep(state = state, viewModel = viewModel)
                4 -> ActivityStep(state = state, viewModel = viewModel)
                5 -> DietaryStep(state = state, viewModel = viewModel)
            }
        }

        if (state.currentStep > 1) {
            if (state.completeFailed) {
                Text(
                    text = stringResource(R.string.onboarding_complete_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
            OnboardingNavButtons(
                currentStep = state.currentStep,
                totalSteps = state.totalSteps,
                canGoNext = state.canGoNext,
                onBack = viewModel::prevStep,
                onNext = viewModel::nextStep,
                onFinish = viewModel::completeOnboarding
            )
        }
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "REPS",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_tagline),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(64.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = stringResource(R.string.onboarding_cta),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun NameAgeStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Column {
        Text(
            text = stringResource(R.string.onboarding_personal_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(32.dp))
        RepsTextField(
            value = state.name,
            onValueChange = viewModel::updateName,
            label = stringResource(R.string.onboarding_name_label),
            placeholder = stringResource(R.string.onboarding_name_placeholder)
        )
        Spacer(Modifier.height(16.dp))
        RepsTextField(
            value = state.age,
            onValueChange = viewModel::updateAge,
            label = stringResource(R.string.onboarding_age_label),
            placeholder = stringResource(R.string.onboarding_age_placeholder),
            keyboardType = KeyboardType.Number
        )
    }
}

@Composable
private fun BodyStatsStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Column {
        Text(
            text = stringResource(R.string.onboarding_body_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(32.dp))
        RepsTextField(
            value = state.weightKg,
            onValueChange = viewModel::updateWeightKg,
            label = stringResource(R.string.onboarding_weight_label),
            placeholder = stringResource(R.string.onboarding_weight_placeholder),
            keyboardType = KeyboardType.Decimal
        )
        Spacer(Modifier.height(16.dp))
        RepsTextField(
            value = state.targetWeightKg,
            onValueChange = viewModel::updateTargetWeightKg,
            label = stringResource(R.string.onboarding_target_weight_label),
            placeholder = stringResource(R.string.onboarding_target_weight_placeholder),
            keyboardType = KeyboardType.Decimal
        )
        Spacer(Modifier.height(16.dp))
        RepsTextField(
            value = state.heightCm,
            onValueChange = viewModel::updateHeightCm,
            label = stringResource(R.string.onboarding_height_label),
            placeholder = stringResource(R.string.onboarding_height_placeholder),
            keyboardType = KeyboardType.Decimal
        )
    }
}

@Composable
private fun ActivityStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    val activityLabels = stringArrayResource(R.array.activity_level_labels)
    val levels = ActivityLevel.entries

    Column {
        Text(
            text = stringResource(R.string.onboarding_activity_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_activity_level_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        levels.forEachIndexed { index, level ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = state.activityLevel == level,
                    onClick = { viewModel.updateActivityLevel(level) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = activityLabels.getOrElse(index) { level.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_workout_days_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_workout_days_value, state.workoutDaysPerWeek),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = state.workoutDaysPerWeek.toFloat(),
            onValueChange = { viewModel.updateWorkoutDays(it.toInt()) },
            valueRange = 1f..7f,
            steps = 5,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.onboarding_shoulder_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = state.hasShoulderRestriction,
                onCheckedChange = { viewModel.toggleShoulderRestriction() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DietaryStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    val dietaryLabels = stringArrayResource(R.array.dietary_restriction_labels)
    val cuisineLabels = stringArrayResource(R.array.cuisine_preference_labels)

    Column {
        Text(
            text = stringResource(R.string.onboarding_dietary_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_optional_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_dietary_restrictions_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OnboardingViewModel.DIETARY_RESTRICTION_KEYS.forEachIndexed { index, key ->
                val label = dietaryLabels.getOrElse(index) { key }
                FilterChip(
                    selected = key in state.selectedDietaryRestrictions,
                    onClick = { viewModel.toggleDietaryRestriction(key) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_cuisine_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OnboardingViewModel.CUISINE_PREFERENCE_KEYS.forEachIndexed { index, key ->
                val label = cuisineLabels.getOrElse(index) { key }
                FilterChip(
                    selected = key in state.selectedCuisinePreferences,
                    onClick = { viewModel.toggleCuisinePreference(key) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun OnboardingNavButtons(
    currentStep: Int,
    totalSteps: Int,
    canGoNext: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.onboarding_back),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = if (currentStep == totalSteps) onFinish else onNext,
            enabled = canGoNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                text = if (currentStep == totalSteps)
                    stringResource(R.string.onboarding_finish)
                else
                    stringResource(R.string.onboarding_next),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun RepsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun OnboardingWelcomePreview() {
    RepsTheme {
        WelcomeStep(onGetStarted = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun OnboardingNavButtonsPreview() {
    RepsTheme {
        OnboardingNavButtons(
            currentStep = 2,
            totalSteps = 5,
            canGoNext = true,
            onBack = {},
            onNext = {},
            onFinish = {}
        )
    }
}
