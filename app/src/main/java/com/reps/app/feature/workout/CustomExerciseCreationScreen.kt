package com.reps.app.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.ai.AiUserMessages
import com.reps.app.ui.components.AiErrorInline
import com.reps.app.ui.components.resolveAiErrorMessage
import com.reps.app.ui.theme.RepsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomExerciseCreationScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: CustomExerciseCreationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMsg = stringResource(R.string.custom_exercise_saved)

    LaunchedEffect(state.savedExerciseId) {
        if (state.savedExerciseId != null) {
            snackbarHostState.showSnackbar(savedMsg)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.custom_exercise_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ExerciseTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.custom_exercise_name_label),
                placeholder = stringResource(R.string.custom_exercise_name_placeholder),
                isError = state.nameError,
                supportingText = if (state.nameError) stringResource(R.string.custom_exercise_name_error) else null
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.custom_exercise_details_heading),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ElevatedButton(
                    onClick = viewModel::estimateDetails,
                    enabled = state.name.isNotBlank() && !state.isEstimating && !state.isSubmitting,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (state.isEstimating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.custom_food_estimating), style = MaterialTheme.typography.labelSmall)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.custom_food_estimate_ai), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            ExerciseTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = stringResource(R.string.custom_exercise_desc_label),
                placeholder = stringResource(R.string.custom_exercise_desc_placeholder)
            )
            Spacer(Modifier.height(12.dp))

            ExerciseTextField(
                value = state.muscleGroups,
                onValueChange = viewModel::onMuscleGroupsChange,
                label = stringResource(R.string.custom_exercise_muscles_label),
                placeholder = stringResource(R.string.custom_exercise_muscles_placeholder)
            )
            Spacer(Modifier.height(12.dp))

            ExerciseTextField(
                value = state.equipment,
                onValueChange = viewModel::onEquipmentChange,
                label = stringResource(R.string.custom_exercise_equipment_label),
                placeholder = stringResource(R.string.custom_exercise_equipment_placeholder)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.custom_exercise_shoulder_safe_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = state.isShoulderSafe,
                    onCheckedChange = viewModel::onShoulderSafeToggle
                )
            }
            Spacer(Modifier.height(12.dp))

            ExerciseTextField(
                value = state.restrictedMovements,
                onValueChange = viewModel::onRestrictedMovementsChange,
                label = stringResource(R.string.custom_exercise_restricted_label),
                placeholder = stringResource(R.string.custom_exercise_restricted_placeholder)
            )

            state.estimationError?.let { errorKey ->
                Spacer(Modifier.height(8.dp))
                AiErrorInline(
                    message = resolveAiErrorMessage(errorKey),
                    onRetry = if (errorKey == AiUserMessages.SAVE_EXERCISE_FAILED) {
                        viewModel::saveExercise
                    } else {
                        viewModel::estimateDetails
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::saveExercise,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting && !state.isEstimating,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text(
                        stringResource(R.string.custom_exercise_save),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ExerciseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        isError = isError,
        supportingText = if (supportingText != null) {
            { Text(supportingText, color = MaterialTheme.colorScheme.error) }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun CustomExerciseCreationScreenPreview() {
    RepsTheme { CustomExerciseCreationScreen() }
}
