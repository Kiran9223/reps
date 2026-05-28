package com.reps.app.feature.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.reps.app.ui.components.ServingsEditDialog
import com.reps.app.ui.theme.RepsTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d")
private val today = LocalDate.now()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealLogScreen(
    onNavigateToDashboard: (date: String) -> Unit = {},
    onNavigateToFoodSearch: (date: String, slot: String) -> Unit = { _, _ -> },
    onNavigateToBarcode: (date: String, slot: String) -> Unit = { _, _ -> },
    onNavigateToNaturalLanguage: (date: String, slot: String) -> Unit = { _, _ -> },
    viewModel: MealLogViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dayLog by viewModel.dayLog.collectAsStateWithLifecycle()
    val pendingDeleteId by viewModel.pendingDeleteId.collectAsStateWithLifecycle()
    val editingEntry by viewModel.editingEntry.collectAsStateWithLifecycle()
    val showAddFoodSheet by viewModel.showAddFoodSheet.collectAsStateWithLifecycle()
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
                title = { Text(stringResource(R.string.screen_diary)) },
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

            DiaryLogCta(
                onAddFood = viewModel::openAddFoodSheet,
                onOpenDashboard = { onNavigateToDashboard(viewModel.getSelectedDateStr()) },
                modifier = Modifier.padding(16.dp)
            )

            MealLogContent(
                dayLog = dayLog,
                pendingDeleteId = pendingDeleteId,
                onDeleteEntry = viewModel::onSwipeToDelete,
                onEditEntry = viewModel::openEditEntry,
                onAddFood = viewModel::openAddFoodSheet,
                modifier = Modifier.weight(1f)
            )
        }
    }

    editingEntry?.let { entry ->
        ServingsEditDialog(
            entry = entry,
            onDismiss = viewModel::closeEditEntry,
            onSave = viewModel::saveEditedServings
        )
    }

    if (showAddFoodSheet) {
        val dateStr = viewModel.getSelectedDateStr()
        DiaryAddFoodSheet(
            onDismiss = viewModel::closeAddFoodSheet,
            onFoodSearch = { slot ->
                viewModel.closeAddFoodSheet()
                onNavigateToFoodSearch(dateStr, slot.name)
            },
            onBarcode = { slot ->
                viewModel.closeAddFoodSheet()
                onNavigateToBarcode(dateStr, slot.name)
            },
            onNaturalLanguage = { slot ->
                viewModel.closeAddFoodSheet()
                onNavigateToNaturalLanguage(dateStr, slot.name)
            }
        )
    }
}

@Composable
private fun DiaryLogCta(
    onAddFood: () -> Unit,
    onOpenDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(onClick = onAddFood, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.diary_add_food))
        }
        TextButton(onClick = onOpenDashboard, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.diary_open_dashboard))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryAddFoodSheet(
    onDismiss: () -> Unit,
    onFoodSearch: (MealSlot) -> Unit,
    onBarcode: (MealSlot) -> Unit,
    onNaturalLanguage: (MealSlot) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pickedSlot by remember { mutableStateOf<MealSlot?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pickedSlot == null) {
                Text(
                    stringResource(R.string.diary_choose_meal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                MealSlot.entries.sortedBy { it.sortOrder }.forEach { slot ->
                    OutlinedButton(
                        onClick = { pickedSlot = slot },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(slot.displayName, fontWeight = FontWeight.SemiBold)
                            Text(
                                slot.timeHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val slot = pickedSlot!!
                TextButton(onClick = { pickedSlot = null }) {
                    Text(stringResource(R.string.diary_back_to_meals))
                }
                Text(
                    slot.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = { onFoodSearch(slot) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search_food), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.slot_add_food_search))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onBarcode(slot) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.cd_scan_barcode), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.slot_add_food_scan), style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(onClick = { onNaturalLanguage(slot) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.TextFields, contentDescription = stringResource(R.string.cd_describe_meal), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.slot_add_food_nl), style = MaterialTheme.typography.labelMedium)
                    }
                }
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
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_previous_day)
            )
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
                contentDescription = stringResource(R.string.cd_next_day),
                tint = if (selectedDate < today) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun MealLogContent(
    dayLog: DayLog,
    pendingDeleteId: Long?,
    onDeleteEntry: (Long) -> Unit,
    onEditEntry: (LoggedFood) -> Unit,
    onAddFood: () -> Unit,
    modifier: Modifier = Modifier
) {
    val slotsWithEntries = MealSlot.entries
        .sortedBy { it.sortOrder }
        .map { slot ->
            val log = dayLog.slotLog(slot)
            slot to log.copy(entries = log.entries.filter { it.entryId != pendingDeleteId })
        }
        .filter { (_, log) -> log.entries.isNotEmpty() }

    if (slotsWithEntries.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    stringResource(R.string.diary_empty_day),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.diary_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onAddFood) {
                    Text(stringResource(R.string.diary_add_food))
                }
            }
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        slotsWithEntries.forEach { (slot, slotLog) ->
            item(key = slot.name) {
                SlotHeader(slot = slot, slotLog = slotLog)
            }
            items(slotLog.entries, key = { it.entryId }) { entry ->
                SwipeToDeleteLogEntry(
                    entry = entry,
                    onDelete = { onDeleteEntry(entry.entryId) },
                    onEdit = { onEditEntry(entry) }
                )
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
            stringResource(R.string.meal_log_slot_calories, slotLog.totalCalories.toInt()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteLogEntry(
    entry: LoggedFood,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
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
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_entry),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit),
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
