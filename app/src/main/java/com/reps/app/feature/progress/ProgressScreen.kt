package com.reps.app.feature.progress

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.reps.app.R
import com.reps.app.core.domain.model.BodyMeasurement
import com.reps.app.core.domain.model.GoalProgress
import com.reps.app.core.domain.model.Streaks
import com.reps.app.core.domain.model.WeeklyStats
import com.reps.app.core.domain.model.WeightCheckIn
import com.reps.app.ui.theme.RepsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val COLOR_PROTEIN = Color(0xFF4CAF50)
private val COLOR_WORKOUT = Color(0xFFFF9800)
private val COLOR_WATER = Color(0xFF29B6F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onNavigateToBodyMeasurements: () -> Unit = {},
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_progress)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openCheckInSheet,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.checkin_fab_desc))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.progressInsight?.let { insight ->
                item { ProgressInsightCard(insight = insight) }
            }
            item {
                WeightChartCard(
                    history = state.weightHistory,
                    goalProgress = state.goalProgress
                )
            }
            item { StreakSection(streaks = state.streaks) }
            item { WeeklyStatsCard(stats = state.weeklyStats) }
            item {
                MeasurementsSection(
                    measurements = state.measurements,
                    expanded = state.measurementsExpanded,
                    onToggle = viewModel::toggleMeasurements,
                    onLogNew = onNavigateToBodyMeasurements,
                    onFormatDate = viewModel::formatDate
                )
            }
        }
    }

    if (state.showCheckInSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeCheckInSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CheckInSheet(
                state = state,
                lastCheckIn = state.weightHistory.firstOrNull(),
                onWeightChanged = viewModel::onWeightChanged,
                onNotesChanged = viewModel::onNotesChanged,
                onSave = viewModel::saveCheckIn,
                onDismiss = viewModel::closeCheckInSheet
            )
        }
    }
}

@Composable
private fun ProgressInsightCard(insight: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    stringResource(R.string.progress_insight_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun WeightChartCard(history: List<WeightCheckIn>, goalProgress: GoalProgress?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.progress_weight_chart_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            if (history.size < 2) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.progress_no_weight_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val modelProducer = remember { CartesianChartModelProducer() }
                val sorted = remember(history) { history.sortedBy { it.date } }
                val yValues = sorted.map { it.weightKg.toFloat() }

                LaunchedEffect(yValues) {
                    withContext(Dispatchers.Default) {
                        modelProducer.runTransaction { lineSeries { series(yValues) } }
                    }
                }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom()
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )

                goalProgress?.let { gp ->
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ChartLegendItem(
                            label = stringResource(R.string.progress_chart_start, gp.startWeightKg),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ChartLegendItem(
                            label = stringResource(R.string.progress_chart_current, gp.currentWeightKg),
                            color = MaterialTheme.colorScheme.primary
                        )
                        ChartLegendItem(
                            label = stringResource(R.string.progress_chart_target, gp.targetWeightKg),
                            color = COLOR_PROTEIN
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when {
                            gp.isGoalReached -> stringResource(R.string.progress_goal_reached)
                            gp.estimatedWeeksToGoal != null ->
                                stringResource(R.string.progress_projected_weeks, gp.estimatedWeeksToGoal)
                            else -> stringResource(R.string.progress_not_enough_data)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (gp.isGoalReached) COLOR_PROTEIN
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun StreakSection(streaks: Streaks) {
    Column {
        Text(
            stringResource(R.string.streak_section_heading),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StreakCard(
                icon = { Icon(Icons.Filled.Restaurant, contentDescription = null, tint = COLOR_PROTEIN, modifier = Modifier.size(20.dp)) },
                label = stringResource(R.string.streak_protein_label),
                days = streaks.proteinStreakDays,
                color = COLOR_PROTEIN,
                modifier = Modifier.weight(1f)
            )
            StreakCard(
                icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = COLOR_WORKOUT, modifier = Modifier.size(20.dp)) },
                label = stringResource(R.string.streak_workout_label),
                days = streaks.workoutStreakDays,
                color = COLOR_WORKOUT,
                modifier = Modifier.weight(1f)
            )
            StreakCard(
                icon = { Icon(Icons.Filled.LocalDrink, contentDescription = null, tint = COLOR_WATER, modifier = Modifier.size(20.dp)) },
                label = stringResource(R.string.streak_water_label),
                days = streaks.waterStreakDays,
                color = COLOR_WATER,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StreakCard(
    icon: @Composable () -> Unit,
    label: String,
    days: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(
                stringResource(R.string.streak_days_format, days),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WeeklyStatsCard(stats: WeeklyStats) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.weekly_stats_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStatCard(
                    label = stringResource(R.string.weekly_stat_avg_calories),
                    value = stringResource(R.string.weekly_stat_avg_calories_value, stats.avgCaloriesKcal),
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    label = stringResource(R.string.weekly_stat_avg_protein),
                    value = stringResource(R.string.weekly_stat_avg_protein_value, stats.avgProteinG),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStatCard(
                    label = stringResource(R.string.weekly_stat_workouts),
                    value = stringResource(R.string.weekly_stat_workouts_value, stats.workoutsDone, stats.workoutTargetPerWeek),
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    label = stringResource(R.string.weekly_stat_protein_goal),
                    value = stringResource(R.string.weekly_stat_protein_goal_value, stats.proteinGoalDays),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MeasurementsSection(
    measurements: List<BodyMeasurement>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLogNew: () -> Unit,
    onFormatDate: (Long) -> String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.measurements_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onLogNew) {
                        Text(stringResource(R.string.measurements_log_new), style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                if (measurements.isEmpty()) {
                    Text(
                        stringResource(R.string.measurements_no_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    measurements.take(5).forEachIndexed { i, m ->
                        if (i > 0) HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        MeasurementHistoryRow(measurement = m, dateLabel = onFormatDate(m.date))
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementHistoryRow(measurement: BodyMeasurement, dateLabel: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (measurement.hasAnyDecrease) {
                Text("💪", style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            measurement.waistCm?.let { MeasurementChip("W", it, measurement.waistDelta) }
            measurement.chestCm?.let { MeasurementChip("C", it, measurement.chestDelta) }
            measurement.armsCm?.let { MeasurementChip("A", it, measurement.armsDelta) }
            measurement.thighsCm?.let { MeasurementChip("T", it, measurement.thighsDelta) }
        }
    }
}

@Composable
private fun MeasurementChip(prefix: String, value: Double, delta: Double?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "$prefix: ${String.format("%.1f", value)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        if (delta != null) {
            val deltaColor = if (delta < 0) COLOR_PROTEIN else MaterialTheme.colorScheme.error
            val deltaStr = if (delta < 0)
                stringResource(R.string.measurements_delta_decrease, -delta)
            else
                stringResource(R.string.measurements_delta_increase, delta)
            Text(deltaStr, style = MaterialTheme.typography.labelSmall, color = deltaColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckInSheet(
    state: ProgressUiState,
    lastCheckIn: WeightCheckIn?,
    onWeightChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.checkin_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = state.checkInWeightText,
            onValueChange = onWeightChanged,
            label = { Text(stringResource(R.string.checkin_weight_label)) },
            placeholder = { Text(stringResource(R.string.checkin_weight_placeholder)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = state.checkInError != null,
            supportingText = {
                when {
                    state.checkInError != null -> Text(state.checkInError, color = MaterialTheme.colorScheme.error)
                    lastCheckIn == null -> Text(stringResource(R.string.checkin_first))
                    state.checkInWeightText.toDoubleOrNull() != null -> {
                        val delta = state.checkInWeightText.toDouble() - lastCheckIn.weightKg
                        when {
                            delta < 0 -> Text(
                                stringResource(R.string.checkin_delta_lost, delta),
                                color = COLOR_PROTEIN
                            )
                            delta > 0 -> Text(
                                stringResource(R.string.checkin_delta_gained, delta),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.checkInNotesText,
            onValueChange = onNotesChanged,
            label = { Text(stringResource(R.string.checkin_notes_label)) },
            placeholder = { Text(stringResource(R.string.checkin_notes_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.create_plan_cancel))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.checkin_save))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ProgressScreenPreview() {
    RepsTheme { ProgressScreen() }
}
