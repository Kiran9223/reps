package com.reps.app.feature.importplan

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.ai.ImportPlanType
import com.reps.app.ui.theme.RepsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlanScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportPlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_plan_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (state.phase) {
            ImportPhase.IDLE, ImportPhase.ERROR -> InputSection(
                state = state,
                onTypeChange = viewModel::onTypeChange,
                onInputChange = viewModel::onInputChange,
                onParse = viewModel::parsePlan,
                modifier = Modifier.padding(innerPadding)
            )
            ImportPhase.PARSING -> LoadingSection(
                message = stringResource(R.string.import_plan_parsing),
                modifier = Modifier.padding(innerPadding)
            )
            ImportPhase.REVIEW -> ReviewSection(
                state = state,
                onWorkoutNameChange = viewModel::updateWorkoutPlanName,
                onRemoveWorkoutDay = viewModel::removeWorkoutDay,
                onRemoveExercise = viewModel::removeExercise,
                onMealNameChange = viewModel::updateMealPlanName,
                onRemoveMealDay = viewModel::removeMealDay,
                onRemoveMealFood = viewModel::removeMealFood,
                onConfirm = viewModel::confirmImport,
                onBack = viewModel::retryInput,
                modifier = Modifier.padding(innerPadding)
            )
            ImportPhase.SAVING -> LoadingSection(
                message = stringResource(R.string.import_plan_saving),
                modifier = Modifier.padding(innerPadding)
            )
            ImportPhase.DONE -> DoneSection(
                message = state.statusMessage.orEmpty(),
                onDone = onNavigateBack,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun InputSection(
    state: ImportPlanUiState,
    onTypeChange: (ImportPlanType) -> Unit,
    onInputChange: (String) -> Unit,
    onParse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.planType == ImportPlanType.WORKOUT,
                onClick = { onTypeChange(ImportPlanType.WORKOUT) },
                label = { Text(stringResource(R.string.import_plan_type_workout)) }
            )
            FilterChip(
                selected = state.planType == ImportPlanType.MEAL,
                onClick = { onTypeChange(ImportPlanType.MEAL) },
                label = { Text(stringResource(R.string.import_plan_type_meal)) }
            )
        }

        OutlinedTextField(
            value = state.inputText,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = {
                Text(
                    text = if (state.planType == ImportPlanType.WORKOUT)
                        stringResource(R.string.import_plan_paste_hint_workout)
                    else
                        stringResource(R.string.import_plan_paste_hint_meal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = { Text(stringResource(R.string.import_plan_input_label)) }
        )

        if (state.phase == ImportPhase.ERROR) {
            Text(
                text = state.statusMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onParse,
            enabled = state.inputText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.import_plan_parse_btn))
        }
    }
}

@Composable
private fun LoadingSection(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ReviewSection(
    state: ImportPlanUiState,
    onWorkoutNameChange: (String) -> Unit,
    onRemoveWorkoutDay: (Int) -> Unit,
    onRemoveExercise: (Int, Int) -> Unit,
    onMealNameChange: (String) -> Unit,
    onRemoveMealDay: (Int) -> Unit,
    onRemoveMealFood: (Int, Int, Int) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.import_plan_review_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (state.reviewWorkout != null) {
            val plan = state.reviewWorkout
            item {
                OutlinedTextField(
                    value = plan.name,
                    onValueChange = onWorkoutNameChange,
                    label = { Text(stringResource(R.string.import_plan_plan_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            itemsIndexed(plan.days) { dayIndex, day ->
                WorkoutDayCard(
                    day = day,
                    onRemoveDay = { onRemoveWorkoutDay(dayIndex) },
                    onRemoveExercise = { exIndex -> onRemoveExercise(dayIndex, exIndex) }
                )
            }
        }

        if (state.reviewMeal != null) {
            val plan = state.reviewMeal
            item {
                OutlinedTextField(
                    value = plan.name,
                    onValueChange = onMealNameChange,
                    label = { Text(stringResource(R.string.import_plan_plan_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            itemsIndexed(plan.days) { dayIndex, day ->
                MealDayCard(
                    day = day,
                    onRemoveDay = { onRemoveMealDay(dayIndex) },
                    onRemoveFood = { slotIndex, foodIndex -> onRemoveMealFood(dayIndex, slotIndex, foodIndex) }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.import_plan_confirm_btn))
                }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.import_plan_edit_btn))
                }
            }
        }
    }
}

@Composable
private fun WorkoutDayCard(
    day: ReviewWorkoutDay,
    onRemoveDay: () -> Unit,
    onRemoveExercise: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemoveDay, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.import_plan_remove_day),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            day.exercises.forEachIndexed { index, exercise ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${exercise.sets} × ${exercise.reps}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { onRemoveExercise(index) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.import_plan_remove),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealDayCard(
    day: ReviewMealDay,
    onRemoveDay: () -> Unit,
    onRemoveFood: (slotIndex: Int, foodIndex: Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemoveDay, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.import_plan_remove_day),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            day.slots.forEachIndexed { slotIndex, slot ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = slot.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                slot.foods.forEachIndexed { foodIndex, food ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = food.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${food.quantity.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.1f".format(it) }} ${food.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { onRemoveFood(slotIndex, foodIndex) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.import_plan_remove),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneSection(message: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(onClick = onDone) {
                Text(stringResource(R.string.import_plan_done_btn))
            }
        }
    }
}

@Preview
@Composable
private fun ImportPlanScreenPreview() {
    RepsTheme { ImportPlanScreen(onNavigateBack = {}) }
}
