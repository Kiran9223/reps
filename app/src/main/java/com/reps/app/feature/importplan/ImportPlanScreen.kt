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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.ai.ImportPlanType
import com.reps.app.ui.components.PrivacyNoticeBanner
import com.reps.app.ui.theme.RepsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlanScreen(
    onNavigateBack: () -> Unit,
    onOpenPrivacySettings: () -> Unit = {},
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
            ImportPhase.IDLE -> InputSection(
                state = state,
                onTypeChange = viewModel::onTypeChange,
                onInputChange = viewModel::onInputChange,
                onParse = viewModel::parsePlan,
                onOpenPrivacySettings = onOpenPrivacySettings,
                modifier = Modifier.padding(innerPadding)
            )
            ImportPhase.PARSE_ERROR -> ParseErrorSection(
                onEditText = viewModel::retryInput,
                onTryAgain = viewModel::parsePlan,
                modifier = Modifier.padding(innerPadding)
            )
            ImportPhase.PARSING -> LoadingSection(
                message = stringResource(R.string.import_plan_parsing),
                modifier = Modifier.padding(innerPadding)
            )
            ImportPhase.REVIEW -> ReviewSection(
                state = state,
                canConfirm = viewModel.canConfirmImport(),
                onWorkoutNameChange = viewModel::updateWorkoutPlanName,
                onRemoveWorkoutDay = viewModel::removeWorkoutDay,
                onRemoveExercise = viewModel::removeExercise,
                onExerciseNameChange = viewModel::updateExerciseName,
                onExerciseSetsChange = viewModel::updateExerciseSets,
                onExerciseRepsChange = viewModel::updateExerciseReps,
                onMealNameChange = viewModel::updateMealPlanName,
                onRemoveMealDay = viewModel::removeMealDay,
                onRemoveMealFood = viewModel::removeMealFood,
                onFoodNameChange = viewModel::updateFoodName,
                onFoodQuantityChange = viewModel::updateFoodQuantity,
                onConfirm = viewModel::confirmImport,
                onBack = viewModel::retryInput,
                modifier = Modifier.padding(innerPadding)
            )
            ImportPhase.SAVING -> LoadingSection(
                message = stringResource(R.string.import_plan_saving),
                modifier = Modifier.padding(innerPadding)
            )
            ImportPhase.SUCCESS -> OutcomeSection(
                title = stringResource(R.string.import_outcome_success_title),
                message = state.statusMessage.orEmpty(),
                primaryLabel = stringResource(R.string.import_plan_done_btn),
                onPrimary = onNavigateBack,
                modifier = Modifier.padding(innerPadding)
            )
            ImportPhase.PARTIAL -> OutcomeSection(
                title = stringResource(R.string.import_outcome_partial_title),
                message = state.statusMessage.orEmpty(),
                primaryLabel = stringResource(R.string.import_back_to_review),
                secondaryLabel = stringResource(R.string.import_plan_done_btn),
                onPrimary = viewModel::backToReview,
                onSecondary = onNavigateBack,
                modifier = Modifier.padding(innerPadding)
            )
            ImportPhase.FAILURE -> OutcomeSection(
                title = stringResource(R.string.import_outcome_failure_title),
                message = state.statusMessage ?: stringResource(R.string.import_outcome_failure_body),
                primaryLabel = stringResource(R.string.import_try_again),
                secondaryLabel = stringResource(R.string.import_back_to_review),
                onPrimary = viewModel::confirmImport,
                onSecondary = viewModel::backToReview,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun ParseErrorSection(
    onEditText: () -> Unit,
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.import_parse_error_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.import_parse_error_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.import_try_again))
            }
            TextButton(onClick = onEditText, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.import_edit_text))
            }
        }
    }
}

@Composable
private fun InputSection(
    state: ImportPlanUiState,
    onTypeChange: (ImportPlanType) -> Unit,
    onInputChange: (String) -> Unit,
    onParse: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
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

        if (state.cloudAssistActive) {
            PrivacyNoticeBanner(text = stringResource(R.string.import_plan_cloud_notice))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrivacyNoticeBanner(text = stringResource(R.string.import_plan_cloud_required))
                TextButton(onClick = onOpenPrivacySettings) {
                    Text(stringResource(R.string.import_plan_open_settings))
                }
            }
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

        Button(
            onClick = onParse,
            enabled = state.inputText.isNotBlank() && state.cloudAssistActive,
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
    canConfirm: Boolean,
    onWorkoutNameChange: (String) -> Unit,
    onRemoveWorkoutDay: (Int) -> Unit,
    onRemoveExercise: (Int, Int) -> Unit,
    onExerciseNameChange: (Int, Int, String) -> Unit,
    onExerciseSetsChange: (Int, Int, Int) -> Unit,
    onExerciseRepsChange: (Int, Int, String) -> Unit,
    onMealNameChange: (String) -> Unit,
    onRemoveMealDay: (Int) -> Unit,
    onRemoveMealFood: (Int, Int, Int) -> Unit,
    onFoodNameChange: (Int, Int, Int, String) -> Unit,
    onFoodQuantityChange: (Int, Int, Int, Double) -> Unit,
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
                    onRemoveExercise = { exIndex -> onRemoveExercise(dayIndex, exIndex) },
                    onExerciseNameChange = { exIndex, name -> onExerciseNameChange(dayIndex, exIndex, name) },
                    onExerciseSetsChange = { exIndex, sets -> onExerciseSetsChange(dayIndex, exIndex, sets) },
                    onExerciseRepsChange = { exIndex, reps -> onExerciseRepsChange(dayIndex, exIndex, reps) }
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
                    onRemoveFood = { slotIndex, foodIndex -> onRemoveMealFood(dayIndex, slotIndex, foodIndex) },
                    onFoodNameChange = { slotIndex, foodIndex, name ->
                        onFoodNameChange(dayIndex, slotIndex, foodIndex, name)
                    },
                    onFoodQuantityChange = { slotIndex, foodIndex, qty ->
                        onFoodQuantityChange(dayIndex, slotIndex, foodIndex, qty)
                    }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!canConfirm) {
                    Text(
                        stringResource(R.string.import_confirm_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onConfirm,
                    enabled = canConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
private fun OutcomeSection(
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) {
                Text(primaryLabel)
            }
            if (secondaryLabel != null && onSecondary != null) {
                TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
                    Text(secondaryLabel)
                }
            }
        }
    }
}

@Composable
private fun WorkoutDayCard(
    day: ReviewWorkoutDay,
    onRemoveDay: () -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onExerciseNameChange: (Int, String) -> Unit,
    onExerciseSetsChange: (Int, Int) -> Unit,
    onExerciseRepsChange: (Int, String) -> Unit
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = exercise.name,
                            onValueChange = { onExerciseNameChange(index, it) },
                            label = { Text(stringResource(R.string.import_edit_exercise_name)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = { onRemoveExercise(index) }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.import_plan_remove),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = exercise.sets.toString(),
                            onValueChange = { text ->
                                text.toIntOrNull()?.let { onExerciseSetsChange(index, it) }
                            },
                            label = { Text(stringResource(R.string.import_edit_sets)) },
                            modifier = Modifier.width(88.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = exercise.reps,
                            onValueChange = { onExerciseRepsChange(index, it) },
                            label = { Text(stringResource(R.string.import_edit_reps)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
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
    onRemoveFood: (slotIndex: Int, foodIndex: Int) -> Unit,
    onFoodNameChange: (slotIndex: Int, foodIndex: Int, String) -> Unit,
    onFoodQuantityChange: (slotIndex: Int, foodIndex: Int, Double) -> Unit
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
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = food.name,
                            onValueChange = { onFoodNameChange(slotIndex, foodIndex, it) },
                            label = { Text(stringResource(R.string.import_edit_food_name)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = food.quantity.let {
                                if (it == it.toLong().toDouble()) it.toLong().toString() else "%.1f".format(it)
                            },
                            onValueChange = { text ->
                                text.toDoubleOrNull()?.let { onFoodQuantityChange(slotIndex, foodIndex, it) }
                            },
                            label = { Text(stringResource(R.string.import_edit_quantity)) },
                            modifier = Modifier.width(72.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Text(
                            text = food.unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        IconButton(
                            onClick = { onRemoveFood(slotIndex, foodIndex) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.import_plan_remove),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ImportPlanScreenPreview() {
    RepsTheme { ImportPlanScreen(onNavigateBack = {}) }
}
