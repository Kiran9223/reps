package com.reps.app.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.core.domain.model.WorkoutFocus
import com.reps.app.core.domain.model.WorkoutSummary
import com.reps.app.core.domain.model.WorkoutTemplate
import com.reps.app.ui.theme.RepsTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLogScreen(
    onNavigateToActiveWorkout: (workoutLogId: Long) -> Unit = {},
    onNavigateToWorkoutHistory: (workoutLogId: Long) -> Unit = {},
    onNavigateToExerciseLibrary: () -> Unit = {},
    onNavigateToCreateTemplate: () -> Unit = {},
    onNavigateToEditTemplate: (templateId: Long) -> Unit = {},
    onImportPlan: () -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val quickWorkoutError = stringResource(R.string.ai_quick_workout_error)
    var templateToDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_workout)) },
                actions = {
                    TextButton(onClick = onImportPlan) {
                        Text(
                            stringResource(R.string.import_plan_action),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onNavigateToExerciseLibrary) {
                        Text(
                            stringResource(R.string.workout_exercise_library),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateTemplate,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.workout_template_create_fab))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ShoulderSafeToggle(
                    enabled = state.isShoulderSafeOnly,
                    onToggle = viewModel::toggleShoulderSafe
                )
            }

            item {
                QuickStartCard(
                    isGenerating = state.isGeneratingQuickWorkout,
                    isStartingEmpty = state.isStarting,
                    onBuild = { focus, minutes ->
                        scope.launch {
                            val id = viewModel.generateAndStartQuickWorkout(focus, minutes)
                            if (id != null) {
                                onNavigateToActiveWorkout(id)
                            } else {
                                snackbarHostState.showSnackbar(quickWorkoutError)
                            }
                        }
                    },
                    onStartEmpty = {
                        scope.launch {
                            val id = viewModel.startWorkout(null)
                            onNavigateToActiveWorkout(id)
                        }
                    }
                )
            }

            if (state.templates.isEmpty() && state.recentWorkouts.isNotEmpty()) {
                item {
                    TemplatesEmptyCard(onCreateTemplate = onNavigateToCreateTemplate)
                }
            }

            if (state.templates.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.workout_templates_heading),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(state.templates, key = { "template_${it.id}" }) { template ->
                    TemplateCard(
                        template = template,
                        isStarting = state.isStarting,
                        onStart = {
                            scope.launch {
                                val id = viewModel.startWorkout(template.id)
                                onNavigateToActiveWorkout(id)
                            }
                        },
                        onEdit = { onNavigateToEditTemplate(template.id) },
                        onDelete = { templateToDelete = template }
                    )
                }
            }

            if (state.recentWorkouts.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.workout_recent_heading),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(state.recentWorkouts, key = { "workout_${it.id}" }) { summary ->
                    RecentWorkoutCard(
                        summary = summary,
                        onClick = { onNavigateToWorkoutHistory(summary.id) }
                    )
                }
            }

            if (state.templates.isEmpty() && state.recentWorkouts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.workout_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text(stringResource(R.string.workout_template_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.workout_template_delete_confirm,
                        templateToDelete!!.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTemplate(templateToDelete!!.id)
                        templateToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.workout_template_delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text(stringResource(R.string.create_plan_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun TemplatesEmptyCard(onCreateTemplate: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.workout_templates_empty_history_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.workout_templates_empty_history_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onCreateTemplate, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.workout_templates_create_first))
            }
        }
    }
}

@Composable
private fun ShoulderSafeToggle(enabled: Boolean, onToggle: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.workout_shoulder_safe_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.workout_shoulder_safe_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.workout_shoulder_safe_quick_start_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = enabled, onCheckedChange = { onToggle() })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickStartCard(
    isGenerating: Boolean,
    isStartingEmpty: Boolean,
    onBuild: (WorkoutFocus, Int) -> Unit,
    onStartEmpty: () -> Unit
) {
    var selectedFocus by remember { mutableStateOf(WorkoutFocus.PUSH) }
    var selectedMinutes by remember { mutableIntStateOf(30) }
    val timeBudgets = listOf(15, 30, 45)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.workout_quick_start_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )

            // Time picker
            Text(
                text = stringResource(R.string.quick_workout_time_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                timeBudgets.forEach { mins ->
                    FilterChip(
                        selected = selectedMinutes == mins,
                        onClick = { selectedMinutes = mins },
                        label = { Text(stringResource(R.string.quick_workout_min_format, mins), style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Focus picker
            Text(
                text = stringResource(R.string.quick_workout_focus_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(WorkoutFocus.PUSH, WorkoutFocus.PULL, WorkoutFocus.LEGS, WorkoutFocus.FULL_BODY).forEach { focus ->
                    FilterChip(
                        selected = selectedFocus == focus,
                        onClick = { selectedFocus = focus },
                        label = { Text(focus.displayName, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Actions
            if (isGenerating) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.quick_workout_building), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            } else {
                Button(
                    onClick = { onBuild(selectedFocus, selectedMinutes) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.quick_workout_build))
                }
                TextButton(
                    onClick = onStartEmpty,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isStartingEmpty
                ) {
                    if (isStartingEmpty) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        stringResource(R.string.quick_workout_start_empty),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: WorkoutTemplate,
    isStarting: Boolean,
    onStart: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.workout_template_exercise_count, template.exercises.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.workout_template_edit_action),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.workout_template_delete_action),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Button(onClick = onStart, enabled = !isStarting) {
                Text(stringResource(R.string.workout_start))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentWorkoutCard(summary: WorkoutSummary, onClick: () -> Unit = {}) {
    val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        .format(Date(summary.date))
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (summary.durationMinutes != null) {
                    Text(
                        text = stringResource(R.string.workout_duration_mins, summary.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = stringResource(R.string.workout_sets_exercises, summary.totalSets, summary.totalExercises),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun WorkoutLogScreenPreview() {
    RepsTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.screen_workout),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
