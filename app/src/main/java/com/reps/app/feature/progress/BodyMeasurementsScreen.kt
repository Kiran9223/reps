package com.reps.app.feature.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.core.domain.model.BodyMeasurement
import com.reps.app.ui.theme.RepsTheme

private val COLOR_DECREASE = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMeasurementsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: BodyMeasurementsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_body_measurements)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedVisibility(visible = state.showMotivational, enter = fadeIn(), exit = fadeOut()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.measurements_motivational),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                MeasurementFormCard(
                    state = state,
                    onWaistChanged = viewModel::onWaistChanged,
                    onChestChanged = viewModel::onChestChanged,
                    onArmsChanged = viewModel::onArmsChanged,
                    onThighsChanged = viewModel::onThighsChanged,
                    onNotesChanged = viewModel::onNotesChanged,
                    onSave = viewModel::save
                )
            }

            if (state.history.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.measurements_history_heading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(state.history, key = { "measurement_${it.id}" }) { measurement ->
                    SwipeToDeleteMeasurementCard(
                        measurement = measurement,
                        dateLabel = viewModel.formatDate(measurement.date),
                        onDelete = { viewModel.deleteMeasurement(measurement.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MeasurementFormCard(
    state: BodyMeasurementsUiState,
    onWaistChanged: (String) -> Unit,
    onChestChanged: (String) -> Unit,
    onArmsChanged: (String) -> Unit,
    onThighsChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.measurements_form_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MeasurementField(
                    value = state.waistText,
                    onValueChange = onWaistChanged,
                    label = stringResource(R.string.measurements_waist),
                    modifier = Modifier.weight(1f)
                )
                MeasurementField(
                    value = state.chestText,
                    onValueChange = onChestChanged,
                    label = stringResource(R.string.measurements_chest),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MeasurementField(
                    value = state.armsText,
                    onValueChange = onArmsChanged,
                    label = stringResource(R.string.measurements_arms),
                    modifier = Modifier.weight(1f)
                )
                MeasurementField(
                    value = state.thighsText,
                    onValueChange = onThighsChanged,
                    label = stringResource(R.string.measurements_thighs),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.notesText,
                onValueChange = onNotesChanged,
                label = { Text(stringResource(R.string.measurements_notes)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.saveError != null) {
                Text(
                    state.saveError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.measurements_save))
                }
            }
        }
    }
}

@Composable
private fun MeasurementField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteMeasurementCard(
    measurement: BodyMeasurement,
    dateLabel: String,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
    ) {
        MeasurementHistoryCard(measurement = measurement, dateLabel = dateLabel)
    }
}

@Composable
private fun MeasurementHistoryCard(measurement: BodyMeasurement, dateLabel: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (measurement.hasAnyDecrease) {
                    Text(
                        stringResource(R.string.measurements_motivational).take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = COLOR_DECREASE
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                measurement.waistCm?.let {
                    MeasurementValueItem(
                        label = stringResource(R.string.measurements_waist).substringBefore(" "),
                        valueCm = it,
                        delta = measurement.waistDelta
                    )
                }
                measurement.chestCm?.let {
                    MeasurementValueItem(
                        label = stringResource(R.string.measurements_chest).substringBefore(" "),
                        valueCm = it,
                        delta = measurement.chestDelta
                    )
                }
                measurement.armsCm?.let {
                    MeasurementValueItem(
                        label = stringResource(R.string.measurements_arms).substringBefore(" "),
                        valueCm = it,
                        delta = measurement.armsDelta
                    )
                }
                measurement.thighsCm?.let {
                    MeasurementValueItem(
                        label = stringResource(R.string.measurements_thighs).substringBefore(" "),
                        valueCm = it,
                        delta = measurement.thighsDelta
                    )
                }
            }

            if (!measurement.notes.isNullOrBlank()) {
                Text(
                    measurement.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MeasurementValueItem(label: String, valueCm: Double, delta: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(R.string.measurements_cm_format, valueCm),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (delta != null) {
            val (deltaStr, color) = if (delta < 0)
                stringResource(R.string.measurements_delta_decrease, -delta) to COLOR_DECREASE
            else
                stringResource(R.string.measurements_delta_increase, delta) to MaterialTheme.colorScheme.error
            Text(deltaStr, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun BodyMeasurementsScreenPreview() {
    RepsTheme { BodyMeasurementsScreen() }
}
