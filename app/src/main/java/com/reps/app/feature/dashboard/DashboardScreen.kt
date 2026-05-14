package com.reps.app.feature.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.DayMacros
import com.reps.app.core.domain.model.LoggedFood
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.model.MealSlotLog
import com.reps.app.ui.theme.RepsTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val COLOR_CARBS = Color(0xFF4FC3F7)
private val COLOR_FAT = Color(0xFFFFB74D)
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToFoodSearch: (date: String, slot: String) -> Unit = { _, _ -> },
    onNavigateToBarcode: (date: String, slot: String) -> Unit = { _, _ -> },
    onNavigateToNaturalLanguage: (date: String, slot: String) -> Unit = { _, _ -> },
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_dashboard)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DateNavigationHeader(
                selectedDate = state.selectedDate,
                today = today,
                onPreviousDay = viewModel::onPreviousDay,
                onNextDay = viewModel::onNextDay
            )

            val tabs = DashboardTab.entries.toList()
            TabRow(
                selectedTabIndex = tabs.indexOf(state.selectedTab),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = state.selectedTab == DashboardTab.TODAY,
                    onClick = { viewModel.onTabChange(DashboardTab.TODAY) },
                    text = { Text(stringResource(R.string.dashboard_tab_today), style = MaterialTheme.typography.labelMedium) }
                )
                Tab(
                    selected = state.selectedTab == DashboardTab.PROGRESS,
                    onClick = { viewModel.onTabChange(DashboardTab.PROGRESS) },
                    text = { Text(stringResource(R.string.dashboard_tab_progress), style = MaterialTheme.typography.labelMedium) }
                )
            }

            when (state.selectedTab) {
                DashboardTab.TODAY -> TodayTab(
                    state = state,
                    onSlotClick = viewModel::openSlotSheet,
                    onAddWater = viewModel::addWater
                )
                DashboardTab.PROGRESS -> ProgressTab(state = state)
            }
        }
    }

    if (state.activeSheet != null) {
        val slot = state.activeSheet!!
        val slotLog = state.dayLog.slotLog(slot)
        ModalBottomSheet(
            onDismissRequest = viewModel::closeSlotSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MealSlotSheet(
                slot = slot,
                slotLog = slotLog,
                onSearchFood = {
                    viewModel.closeSlotSheet()
                    onNavigateToFoodSearch(viewModel.getSelectedDateStr(), slot.name)
                },
                onScanBarcode = {
                    viewModel.closeSlotSheet()
                    onNavigateToBarcode(viewModel.getSelectedDateStr(), slot.name)
                },
                onNaturalLanguage = {
                    viewModel.closeSlotSheet()
                    onNavigateToNaturalLanguage(viewModel.getSelectedDateStr(), slot.name)
                },
                onDeleteEntry = viewModel::removeLogEntry
            )
        }
    }
}

@Composable
private fun DateNavigationHeader(
    selectedDate: LocalDate,
    today: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground)
        }
        val label = when (selectedDate) {
            today -> stringResource(R.string.date_today)
            today.minusDays(1) -> stringResource(R.string.date_yesterday)
            else -> selectedDate.format(DATE_FORMATTER)
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(
            onClick = onNextDay,
            enabled = selectedDate < today
        ) {
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
private fun TodayTab(
    state: DashboardUiState,
    onSlotClick: (MealSlot) -> Unit,
    onAddWater: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            MacroRingSection(macros = state.macros, modifier = Modifier.padding(16.dp))
        }
        item {
            CalorieWaterSection(
                macros = state.macros,
                waterMl = state.waterMl,
                waterTarget = state.waterTarget,
                onAddWater = onAddWater,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
        items(MealSlot.entries.sortedBy { it.sortOrder }) { slot ->
            SlotCard(
                slot = slot,
                slotLog = state.dayLog.slotLog(slot),
                onClick = { onSlotClick(slot) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        item {
            InsightCard(modifier = Modifier.padding(16.dp))
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun MacroRingSection(macros: DayMacros, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 18.dp.toPx()
            val gap = 6.dp.toPx()
            val maxRadius = (size.minDimension / 2f) - strokeWidth / 2f

            fun drawRing(radius: Float, progress: Float, color: Color) {
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                drawArc(surfaceVariant, -90f, 360f, false, topLeft, arcSize, style = style)
                if (progress > 0f) {
                    drawArc(color, -90f, progress * 360f, false, topLeft, arcSize, style = style)
                }
            }

            drawRing(maxRadius, macros.proteinProgress, primaryColor)
            drawRing(maxRadius - strokeWidth - gap, macros.carbsProgress, COLOR_CARBS)
            drawRing(maxRadius - (strokeWidth + gap) * 2, macros.fatProgress, COLOR_FAT)
        }

        Spacer(Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroLegendRow(
                color = primaryColor,
                label = stringResource(R.string.dashboard_protein),
                consumed = macros.protein.toInt(),
                target = macros.targets.proteinG
            )
            MacroLegendRow(
                color = COLOR_CARBS,
                label = stringResource(R.string.dashboard_carbs),
                consumed = macros.carbs.toInt(),
                target = macros.targets.carbsG
            )
            MacroLegendRow(
                color = COLOR_FAT,
                label = stringResource(R.string.dashboard_fat),
                consumed = macros.fat.toInt(),
                target = macros.targets.fatG
            )
        }
    }
}

@Composable
private fun MacroLegendRow(color: Color, label: String, consumed: Int, target: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.dashboard_macro_grams, consumed, target),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CalorieWaterSection(
    macros: DayMacros,
    waterMl: Int,
    waterTarget: Int,
    onAddWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.dashboard_calories), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.dashboard_calories_format, macros.calories.toInt(), macros.targets.calories),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LinearProgressIndicator(
                    progress = { macros.calorieProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = primaryColor,
                    trackColor = surfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.dashboard_water), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(R.string.dashboard_water_format, waterMl, waterTarget),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val waterProgress = if (waterTarget > 0) (waterMl.toFloat() / waterTarget).coerceAtMost(1f) else 0f
                    LinearProgressIndicator(
                        progress = { waterProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = COLOR_CARBS,
                        trackColor = surfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = onAddWater,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.dashboard_add_water), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SlotCard(
    slot: MealSlot,
    slotLog: MealSlotLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(slot.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(slot.timeHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                if (slotLog.entries.isEmpty()) {
                    Text(
                        stringResource(R.string.dashboard_slot_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        stringResource(R.string.dashboard_slot_kcal, slotLog.totalCalories.toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.slot_total_macros,
                            slotLog.totalProtein, slotLog.totalCarbs, slotLog.totalFat),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun InsightCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.dashboard_insight_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.dashboard_insight_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgressTab(state: DashboardUiState) {
    if (state.weekHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.progress_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                stringResource(R.string.progress_7day_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = stringResource(R.string.progress_protein_goal),
                    value = stringResource(R.string.progress_protein_goal_value, state.weekStats.proteinHitDays),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.progress_streak),
                    value = stringResource(R.string.progress_streak_value, state.weekStats.currentStreak),
                    color = COLOR_FAT,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            StatCard(
                label = stringResource(R.string.progress_avg_calories),
                value = stringResource(R.string.progress_avg_calories_value, state.weekStats.avgCalories),
                color = COLOR_CARBS,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
        items(state.weekHistory.sortedByDescending { it.date }) { dayLog ->
            WeekDayRow(dayLog = dayLog, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun WeekDayRow(dayLog: DayLog, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(dayLog.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${dayLog.totalCalories.toInt()} kcal  ·  P ${dayLog.totalProtein.toInt()}g",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealSlotSheet(
    slot: MealSlot,
    slotLog: MealSlotLog,
    onSearchFood: () -> Unit,
    onScanBarcode: () -> Unit,
    onNaturalLanguage: () -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(slot.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(slot.timeHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.slot_total_macros,
                    slotLog.totalProtein, slotLog.totalCarbs, slotLog.totalFat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))

        if (slotLog.entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.slot_no_entries),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            slotLog.entries.forEach { entry ->
                SwipeToDeleteEntry(
                    entry = entry,
                    onDelete = { onDeleteEntry(entry.entryId) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSearchFood,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.slot_add_food_search))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onScanBarcode,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.slot_add_food_scan), style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onNaturalLanguage,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.slot_add_food_nl), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteEntry(
    entry: LoggedFood,
    onDelete: () -> Unit
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
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.food.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.slot_entry_detail, entry.servingMultiplier, entry.calories),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${entry.calories.toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun DashboardScreenPreview() {
    RepsTheme { DashboardScreen() }
}
