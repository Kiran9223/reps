package com.reps.app.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.ui.components.AiErrorInline
import com.reps.app.ui.components.resolveAiErrorMessage
import com.reps.app.ui.theme.RepsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutTemplateScreen(
    pendingExerciseId: Long = -1L,
    pendingExerciseName: String = "",
    onPendingConsumed: () -> Unit = {},
    onNavigateToExercisePicker: () -> Unit = {},
    onSaved: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: CreateWorkoutTemplateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val savedMsg = stringResource(R.string.workout_template_saved)

    LaunchedEffect(pendingExerciseId) {
        if (pendingExerciseId >= 0L && pendingExerciseName.isNotBlank()) {
            viewModel.addExercise(pendingExerciseId, pendingExerciseName)
            onPendingConsumed()
        }
    }

    LaunchedEffect(state.savedTemplateId) {
        if (state.savedTemplateId != null) {
            snackbarHost.showSnackbar(savedMsg)
            onSaved()
        }
    }

    val isEditMode = state.templateId != null
    val titleRes = if (isEditMode) R.string.workout_template_edit_title else R.string.workout_template_create_title

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.create_plan_cancel))
                    }
                },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        TextButton(
                            onClick = viewModel::saveTemplate,
                            enabled = !state.isSaving
                        ) {
                            Text(
                                stringResource(R.string.workout_template_save),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::setName,
                        label = { Text(stringResource(R.string.workout_template_name_label)) },
                        placeholder = { Text(stringResource(R.string.workout_template_name_hint)) },
                        isError = state.nameError,
                        supportingText = if (state.nameError) {
                            { Text(stringResource(R.string.workout_template_name_error), color = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = viewModel::setDescription,
                        label = { Text(stringResource(R.string.workout_template_desc_label)) },
                        placeholder = { Text(stringResource(R.string.workout_template_desc_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.workout_template_exercises_heading),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ElevatedButton(
                            onClick = viewModel::estimateWorkout,
                            enabled = state.name.isNotBlank() && !state.isEstimating && !state.isSaving,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (state.isEstimating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.workout_template_generating),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.workout_template_generate_ai),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    state.estimationError?.let { errorKey ->
                        AiErrorInline(
                            message = resolveAiErrorMessage(errorKey),
                            onRetry = viewModel::estimateWorkout
                        )
                    }
                }
            }

            if (state.exercises.isNotEmpty()) {
                items(state.exercises, key = { it.exerciseId }) { item ->
                    DraftExerciseCard(
                        item = item,
                        onRemove = { viewModel.removeExercise(item.exerciseId) },
                        onSetsChange = { viewModel.updateExerciseSets(item.exerciseId, it) },
                        onRepsChange = { viewModel.updateExerciseReps(item.exerciseId, it) },
                        onWeightChange = { viewModel.updateExerciseWeight(item.exerciseId, it) }
                    )
                }
            }

            if (state.noExerciseError) {
                item(key = "no_exercise_error") {
                    Text(
                        text = stringResource(R.string.workout_template_no_exercise_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            item(key = "add_exercise_button") {
                TextButton(
                    onClick = onNavigateToExercisePicker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.workout_template_add_exercise),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftExerciseCard(
    item: DraftExerciseItem,
    onRemove: () -> Unit,
    onSetsChange: (Int) -> Unit,
    onRepsChange: (String) -> Unit,
    onWeightChange: (Double?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.workout_template_remove_exercise),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.targetSets.toString(),
                    onValueChange = { it.toIntOrNull()?.let(onSetsChange) },
                    label = { Text(stringResource(R.string.workout_template_sets_label), style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = item.targetReps,
                    onValueChange = onRepsChange,
                    label = { Text(stringResource(R.string.workout_template_reps_label), style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = item.targetWeightKg?.toString() ?: "",
                    onValueChange = { onWeightChange(it.toDoubleOrNull()) },
                    label = { Text(stringResource(R.string.workout_template_weight_label), style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun CreateWorkoutTemplateScreenPreview() {
    RepsTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Create Workout Template",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
