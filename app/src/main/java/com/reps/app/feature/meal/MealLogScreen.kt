package com.reps.app.feature.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.LoggedFood
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.model.MealSlotLog
import com.reps.app.ui.theme.RepsTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d")
private val today = LocalDate.now()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealLogScreen(
    viewModel: MealLogViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dayLog by viewModel.dayLog.collectAsStateWithLifecycle()
    val pendingDeleteId by viewModel.pendingDeleteId.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.undo)
    val deletedMsg = stringResource(R.string.undo_delete_food)

    LaunchedEffect(pendingDeleteId) {
        if (pendingDeleteId == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedMsg,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        else viewModel.commitDelete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_meal_log)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DateNavigationRow(
                selectedDate = selectedDate,
                onPrevious = viewModel::onPreviousDay,
                onNext = viewModel::onNextDay
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                MealLogContent(
                    dayLog = dayLog,
                    pendingDeleteId = pendingDeleteId,
                    onDeleteEntry = viewModel::onSwipeToDelete
                )
            }
        }
    }
}

@Composable
private fun DateNavigationRow(
    selectedDate: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
        }
        val label = when (selectedDate) {
            today -> stringResource(R.string.date_today)
            today.minusDays(1) -> stringResource(R.string.date_yesterday)
            else -> selectedDate.format(DATE_FMT)
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext, enabled = selectedDate < today) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (selectedDate < today) MaterialTheme.colorScheme.onBackground
                       else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun MealLogContent(dayLog: DayLog, pendingDeleteId: Long?, onDeleteEntry: (Long) -> Unit) {
    val slotsWithEntries = MealSlot.entries
        .sortedBy { it.sortOrder }
        .map { slot ->
            val log = dayLog.slotLog(slot)
            slot to log.copy(entries = log.entries.filter { it.entryId != pendingDeleteId })
        }
        .filter { (_, log) -> log.entries.isNotEmpty() }

    if (slotsWithEntries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.meal_log_no_entries),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        slotsWithEntries.forEach { (slot, slotLog) ->
            item(key = slot.name) {
                SlotHeader(slot = slot, slotLog = slotLog)
            }
            items(slotLog.entries, key = { it.entryId }) { entry ->
                SwipeToDeleteLogEntry(entry = entry, onDelete = { onDeleteEntry(entry.entryId) })
            }
            item(key = "${slot.name}_divider") {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun SlotHeader(slot: MealSlot, slotLog: MealSlotLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(slot.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.meal_log_slot_header, slot.displayName, slotLog.totalCalories.toInt()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteLogEntry(entry: LoggedFood, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDelete()
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.food.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${entry.servingMultiplier}× ${entry.food.servingDescription}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${entry.calories.toInt()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "P ${entry.protein.toInt()}g  C ${entry.carbs.toInt()}g  F ${entry.fat.toInt()}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun MealLogScreenPreview() {
    RepsTheme { MealLogScreen() }
}
