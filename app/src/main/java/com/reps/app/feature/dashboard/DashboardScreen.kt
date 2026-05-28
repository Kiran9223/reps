package com.reps.app.feature.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.ai.AiMealSuggestion
import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.DayMacros
import com.reps.app.core.domain.model.GoalProgress
import com.reps.app.core.domain.model.LoggedFood
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.model.MealSlotLog
import com.reps.app.ui.components.AiErrorInline
import com.reps.app.ui.components.ServingsEditDialog
import com.reps.app.ui.theme.MacroColors
import com.reps.app.ui.theme.RepsTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    initialFocusDate: String? = null,
    onNavigateToFoodSearch: (date: String, slot: String) -> Unit = { _, _ -> },
    onNavigateToBarcode: (date: String, slot: String) -> Unit = { _, _ -> },
    onNavigateToNaturalLanguage: (date: String, slot: String) -> Unit = { _, _ -> },
    onNavigateToProgress: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val undoLabel = stringResource(R.string.undo)
    val deletedMsg = stringResource(R.string.undo_delete_food)

    LaunchedEffect(initialFocusDate) {
        if (!initialFocusDate.isNullOrBlank()) {
            runCatching { LocalDate.parse(initialFocusDate) }
                .getOrNull()
                ?.let { viewModel.setSelectedDate(it) }
        }
    }

    // Haptic feedback when protein goal is reached
    val proteinGoalReached = state.macros.proteinProgress >= 1f
    var showProteinCelebration by remember { mutableStateOf(false) }
    LaunchedEffect(proteinGoalReached) {
        if (!proteinGoalReached) return@LaunchedEffect
        if (viewModel.hasProteinCelebrationToday()) return@LaunchedEffect
        showProteinCelebration = true
        viewModel.markProteinCelebrated()
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LaunchedEffect(state.selectedDate, state.selectedTab) {
        if (state.selectedTab == DashboardTab.TODAY) {
            viewModel.tryAutoFetchInsightIfNeeded()
        }
    }

    // Undo snackbar for swipe-to-delete
    LaunchedEffect(state.pendingDeleteEntryId) {
        if (state.pendingDeleteEntryId == null) return@LaunchedEffect
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
                title = { Text(stringResource(R.string.screen_dashboard)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            AnimatedContent(
                targetState = state.selectedTab,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "dashboard_tab"
            ) { tab ->
                when (tab) {
                    DashboardTab.TODAY -> TodayTab(
                        state = state,
                        showProteinCelebration = showProteinCelebration,
                        onDismissProteinCelebration = { showProteinCelebration = false },
                        onSlotClick = viewModel::openSlotSheet,
                        onAddWater = viewModel::addWater,
                        onNavigateToProgress = onNavigateToProgress,
                        onNavigateToSettings = onNavigateToSettings,
                        onRefreshAll = viewModel::onRefreshAll,
                        onRetryInsight = viewModel::requestInsight
                    )
                    DashboardTab.PROGRESS -> ProgressTab(
                        state = state,
                        onNavigateToProgress = onNavigateToProgress,
                        onLogOnDashboard = { viewModel.onTabChange(DashboardTab.TODAY) }
                    )
                }
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
                suggestion = state.mealSuggestion,
                isFetchingSuggestion = state.isFetchingSuggestion,
                suggestionFailed = state.suggestionFailed,
                onRetrySuggestion = viewModel::requestMealSuggestion,
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
                onDeleteEntry = viewModel::onSwipeToDelete,
                onEditEntry = viewModel::openEditEntry,
                onSuggestMeal = viewModel::requestMealSuggestion,
                onAddSuggestion = viewModel::addSuggestedMealToLog
            )
        }
    }

    state.editingEntry?.let { entry ->
        ServingsEditDialog(
            entry = entry,
            onDismiss = viewModel::closeEditEntry,
            onSave = viewModel::saveEditedServings
        )
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
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_previous_day),
                tint = MaterialTheme.colorScheme.onBackground)
        }
        val label = when (selectedDate) {
            today -> stringResource(R.string.date_today)
            today.minusDays(1) -> stringResource(R.string.date_yesterday)
            else -> selectedDate.format(DATE_FORMATTER)
        }
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(
            onClick = onNextDay,
            enabled = selectedDate < today
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_next_day),
                tint = if (selectedDate < today) MaterialTheme.colorScheme.onBackground
                       else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayTab(
    state: DashboardUiState,
    showProteinCelebration: Boolean,
    onDismissProteinCelebration: () -> Unit,
    onSlotClick: (MealSlot) -> Unit,
    onAddWater: (Int) -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRefreshAll: () -> Unit,
    onRetryInsight: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val onAddWaterWithHaptic: (Int) -> Unit = { ml ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onAddWater(ml)
    }
    PullToRefreshBox(
        isRefreshing = state.isRefreshingAll || state.isFetchingInsight,
        onRefresh = onRefreshAll,
        modifier = Modifier.fillMaxSize()
    ) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (showProteinCelebration) {
            item {
                ProteinGoalCelebrationBanner(
                    onDismiss = onDismissProteinCelebration,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        item {
            MacroHeroCard(
                macros = state.macros,
                waterMl = state.waterMl,
                waterTarget = state.waterTarget,
                onAddWater = onAddWaterWithHaptic,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
        if (state.goalProgress != null) {
            item {
                GoalProgressCard(
                    goalProgress = state.goalProgress,
                    onTrackProgress = onNavigateToProgress,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(4.dp))
            }
        } else if (!state.isProfileComplete) {
            item {
                ProfileIncompleteCard(
                    onOpenSettings = onNavigateToSettings,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        items(MealSlot.entries.sortedBy { it.sortOrder }, key = { it.name }) { slot ->
            SlotCard(
                slot = slot,
                slotLog = state.dayLog.slotLog(slot),
                onClick = { onSlotClick(slot) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        item {
            InsightCard(
                insight = state.dailyInsight,
                isFetching = state.isFetchingInsight,
                hasError = state.insightFailed,
                onRefresh = onRetryInsight,
                modifier = Modifier.padding(16.dp)
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    } // end PullToRefreshBox
}

@Composable
private fun ProteinGoalCelebrationBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.protein_goal_celebration_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(R.string.protein_goal_celebration_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ai_coach_action_dismiss))
            }
        }
    }
}

@Composable
private fun MacroHeroCard(
    macros: DayMacros,
    waterMl: Int,
    waterTarget: Int,
    onAddWater: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val proteinGoalReached = macros.proteinProgress >= 1f
    val calorieGoalReached = macros.calorieProgress >= 1f
    val waterProgress = if (waterTarget > 0) (waterMl.toFloat() / waterTarget).coerceAtMost(1f) else 0f
    val waterGoalReached = waterTarget > 0 && waterMl >= waterTarget

    var proteinPulseTarget by remember { mutableStateOf(1f) }
    LaunchedEffect(proteinGoalReached) {
        if (!proteinGoalReached) return@LaunchedEffect
        proteinPulseTarget = 1.07f
        delay(320)
        proteinPulseTarget = 1f
    }
    val ringScale by animateFloatAsState(
        targetValue = proteinPulseTarget,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "proteinPulse"
    )

    val ringSpec = tween<Float>(durationMillis = 900, easing = FastOutSlowInEasing)
    val animatedProtein by animateFloatAsState(macros.proteinProgress, ringSpec, label = "protein")
    val animatedCarbs by animateFloatAsState(
        macros.carbsProgress, tween(900, delayMillis = 80, easing = FastOutSlowInEasing), label = "carbs"
    )
    val animatedFat by animateFloatAsState(
        macros.fatProgress, tween(900, delayMillis = 160, easing = FastOutSlowInEasing), label = "fat"
    )

    val caloriesRemaining = (macros.targets.calories - macros.calories.toInt()).coerceAtLeast(0)
    val macroRingDescription = stringResource(
        R.string.cd_macro_rings,
        macros.protein.toInt(),
        macros.targets.proteinG,
        macros.carbs.toInt(),
        macros.targets.carbsG,
        macros.fat.toInt(),
        macros.targets.fatG
    )
    val calorieDesc = stringResource(R.string.cd_calorie_progress, macros.calories.toInt(), macros.targets.calories)
    val waterDesc = stringResource(R.string.cd_water_progress, waterMl, waterTarget)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(
                    modifier = Modifier
                        .size(148.dp)
                        .scale(ringScale)
                        .semantics { contentDescription = macroRingDescription }
                ) {
                    val strokeWidth = 16.dp.toPx()
                    val gap = 5.dp.toPx()
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

                    drawRing(maxRadius, animatedProtein, MacroColors.Protein)
                    drawRing(maxRadius - strokeWidth - gap, animatedCarbs, MacroColors.Carbs)
                    drawRing(maxRadius - (strokeWidth + gap) * 2, animatedFat, MacroColors.Fat)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.dashboard_calories),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (calorieGoalReached) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = stringResource(R.string.dashboard_macro_goal_met),
                                tint = MacroColors.GoalMet,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "${macros.calories.toInt()}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            R.string.dashboard_calories_format,
                            macros.calories.toInt(),
                            macros.targets.calories
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!calorieGoalReached && caloriesRemaining > 0) {
                        Text(
                            stringResource(R.string.dashboard_calories_remaining, caloriesRemaining),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (calorieGoalReached) {
                        Text(
                            stringResource(R.string.dashboard_macro_goal_met),
                            style = MaterialTheme.typography.labelSmall,
                            color = MacroColors.GoalMet
                        )
                    }
                    LinearProgressIndicator(
                        progress = { macros.calorieProgress.coerceAtMost(1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .semantics { contentDescription = calorieDesc },
                        color = MacroColors.Protein,
                        trackColor = surfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroLegendRow(
                    color = MacroColors.Protein,
                    label = stringResource(R.string.dashboard_protein),
                    consumed = macros.protein.toInt(),
                    target = macros.targets.proteinG,
                    modifier = Modifier.weight(1f)
                )
                MacroLegendRow(
                    color = MacroColors.Carbs,
                    label = stringResource(R.string.dashboard_carbs),
                    consumed = macros.carbs.toInt(),
                    target = macros.targets.carbsG,
                    modifier = Modifier.weight(1f)
                )
                MacroLegendRow(
                    color = MacroColors.Fat,
                    label = stringResource(R.string.dashboard_fat),
                    consumed = macros.fat.toInt(),
                    target = macros.targets.fatG,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.dashboard_water),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (waterGoalReached) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = stringResource(R.string.dashboard_macro_goal_met),
                                tint = MacroColors.GoalMet,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.dashboard_water_format, waterMl, waterTarget),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (waterGoalReached) MacroColors.GoalMet else MaterialTheme.colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { waterProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .semantics { contentDescription = waterDesc },
                    color = MacroColors.Water,
                    trackColor = surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { onAddWater(200) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dashboard_add_water_200), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { onAddWater(500) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dashboard_add_water_500), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { onAddWater(1000) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dashboard_add_water_1l), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroLegendRow(
    color: Color,
    label: String,
    consumed: Int,
    target: Int,
    modifier: Modifier = Modifier
) {
    val remaining = target - consumed
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
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
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (remaining > 0) {
                Text(
                    stringResource(R.string.dashboard_macro_remaining, remaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.dashboard_macro_goal_met),
                    style = MaterialTheme.typography.labelSmall,
                    color = MacroColors.GoalMet
                )
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
    val addCd = stringResource(R.string.dashboard_slot_add_a11y, slot.displayName)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(slot.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(slot.timeHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                if (slotLog.entries.isEmpty()) {
                    Text(
                        stringResource(R.string.dashboard_slot_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        stringResource(R.string.dashboard_slot_kcal, slotLog.totalCalories.toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MacroColors.Protein,
                        fontWeight = FontWeight.Bold
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
                contentDescription = addCd,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun InsightCard(
    insight: String?,
    isFetching: Boolean,
    hasError: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = stringResource(R.string.dashboard_insight_title),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        stringResource(R.string.dashboard_insight_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh_insight),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                when {
                    hasError -> AiErrorInline(
                        message = stringResource(R.string.ai_insight_error),
                        onRetry = onRefresh
                    )
                    isFetching && insight == null -> InsightLoadingSkeleton()
                    insight != null -> Text(
                        text = insight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    else -> {
                        Column {
                            Text(
                                text = stringResource(R.string.dashboard_insight_placeholder),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onRefresh) {
                                Text(stringResource(R.string.dashboard_insight_get))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressTab(
    state: DashboardUiState,
    onNavigateToProgress: () -> Unit,
    onLogOnDashboard: () -> Unit
) {
    if (state.weekHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    stringResource(R.string.progress_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onLogOnDashboard) {
                    Text(stringResource(R.string.progress_no_data_cta))
                }
                OutlinedButton(onClick = onNavigateToProgress) {
                    Text(stringResource(R.string.progress_open_full))
                }
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    stringResource(R.string.progress_7day_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.dashboard_progress_tab_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateToProgress, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.progress_open_full))
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = stringResource(R.string.progress_protein_goal),
                    value = stringResource(R.string.progress_protein_goal_value, state.weekStats.proteinHitDays),
                    color = MacroColors.Protein,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.progress_streak),
                    value = stringResource(R.string.progress_streak_value, state.weekStats.currentStreak),
                    color = MacroColors.Fat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            StatCard(
                label = stringResource(R.string.progress_avg_calories),
                value = stringResource(R.string.progress_avg_calories_value, state.weekStats.avgCalories),
                color = MacroColors.Carbs,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
        items(state.weekHistory.sortedByDescending { it.date }, key = { it.date }) { dayLog ->
            WeekDayRow(dayLog = dayLog, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun InsightLoadingSkeleton() {
    val placeholder = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(placeholder, RoundedCornerShape(4.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(12.dp)
                .background(placeholder, RoundedCornerShape(4.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(12.dp)
                .background(placeholder, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun WeekDayRow(dayLog: DayLog, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(dayLog.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.dashboard_slot_kcal, dayLog.totalCalories.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "P ${dayLog.totalProtein.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MacroColors.Protein
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotAddActionTile(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    if (emphasized) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    } else {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealSlotSheet(
    slot: MealSlot,
    slotLog: MealSlotLog,
    suggestion: AiMealSuggestion?,
    isFetchingSuggestion: Boolean,
    suggestionFailed: Boolean,
    onRetrySuggestion: () -> Unit,
    onSearchFood: () -> Unit,
    onScanBarcode: () -> Unit,
    onNaturalLanguage: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onEditEntry: (LoggedFood) -> Unit,
    onSuggestMeal: () -> Unit,
    onAddSuggestion: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(slot.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(slot.timeHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.dashboard_slot_kcal, slotLog.totalCalories.toInt()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MacroColors.Protein
            )
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
                    onDelete = { onDeleteEntry(entry.entryId) },
                    onEdit = { onEditEntry(entry) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))

        // AI Meal suggestion card
        if (suggestion != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                        Text(stringResource(R.string.slot_suggestion_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(
                        stringResource(R.string.slot_suggestion_reason_format,
                            suggestion.foodName, suggestion.servings, suggestion.reason),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onAddSuggestion,
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            stringResource(R.string.slot_suggestion_add),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SlotAddActionTile(
                icon = Icons.Default.Search,
                label = stringResource(R.string.slot_add_food_search),
                contentDescription = stringResource(R.string.cd_search_food),
                onClick = onSearchFood,
                modifier = Modifier.fillMaxWidth(),
                emphasized = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SlotAddActionTile(
                    icon = Icons.Default.QrCodeScanner,
                    label = stringResource(R.string.slot_add_food_scan),
                    contentDescription = stringResource(R.string.cd_scan_barcode),
                    onClick = onScanBarcode,
                    modifier = Modifier.weight(1f)
                )
                SlotAddActionTile(
                    icon = Icons.Default.TextFields,
                    label = stringResource(R.string.slot_add_food_nl),
                    contentDescription = stringResource(R.string.cd_describe_meal),
                    onClick = onNaturalLanguage,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedButton(
                onClick = onSuggestMeal,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFetchingSuggestion
            ) {
                if (isFetchingSuggestion) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = stringResource(R.string.cd_suggest_meal),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.slot_suggest_meal), style = MaterialTheme.typography.labelMedium)
            }
            if (suggestionFailed) {
                AiErrorInline(
                    message = stringResource(R.string.ai_suggestion_error),
                    onRetry = onRetrySuggestion
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteEntry(
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
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete_entry), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        Card(
            onClick = onEdit,
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

@Composable
private fun ProfileIncompleteCard(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.goal_profile_incomplete_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.goal_profile_incomplete_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.goal_profile_incomplete_cta))
            }
        }
    }
}

@Composable
private fun GoalProgressCard(
    goalProgress: GoalProgress,
    onTrackProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goalColor = Color(0xFF4CAF50)
    Card(
        modifier = modifier.fillMaxWidth(),
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
                    stringResource(R.string.goal_progress_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onTrackProgress, modifier = Modifier.height(28.dp)) {
                    Text(
                        stringResource(R.string.goal_progress_track),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    stringResource(R.string.goal_progress_current_format, goalProgress.currentWeightKg),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.goal_progress_target_format, goalProgress.targetWeightKg),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { goalProgress.progressFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = goalColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (goalProgress.isGoalReached) stringResource(R.string.goal_progress_achieved)
                    else stringResource(R.string.goal_progress_remaining, goalProgress.kgRemaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (goalProgress.isGoalReached) goalColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                goalProgress.estimatedWeeksToGoal?.let { weeks ->
                    Text(
                        stringResource(R.string.goal_progress_weeks, weeks),
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
private fun DashboardScreenPreview() {
    RepsTheme { DashboardScreen() }
}
