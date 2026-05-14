package com.reps.app.feature.meal

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.ui.theme.RepsTheme

private val DAY_LABELS = listOf("Mon / Thu", "Tue / Fri", "Wed / Sat", "Sunday")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMealPlanScreen(
    pendingFoodId: Long = -1L,
    pendingFoodName: String = "",
    pendingServings: Double = 1.0,
    pendingDayIndex: Int = -1,
    pendingSlotName: String = "",
    onPendingConsumed: () -> Unit = {},
    onNavigateToFoodSearch: (dayIndex: Int, slot: MealSlot) -> Unit = { _, _ -> },
    onSaved: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: CreateMealPlanViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val savedMsg = stringResource(R.string.create_plan_saved)

    LaunchedEffect(pendingFoodId) {
        if (pendingFoodId >= 0L && pendingDayIndex >= 0 && pendingSlotName.isNotBlank()) {
            val slot = runCatching { MealSlot.valueOf(pendingSlotName) }.getOrNull()
            if (slot != null) {
                viewModel.addFoodToSlot(pendingDayIndex, slot, pendingFoodId, pendingFoodName, pendingServings)
            }
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
    val titleRes = if (isEditMode) R.string.screen_edit_meal_plan else R.string.screen_create_meal_plan

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
                            onClick = viewModel::savePlan,
                            enabled = !state.isSaving
                        ) {
                            Text(
                                stringResource(R.string.create_plan_save),
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
            item { PlanFormHeader(state = state, viewModel = viewModel) }

            item {
                ScrollableTabRow(
                    selectedTabIndex = state.selectedDayIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 0.dp
                ) {
                    DAY_LABELS.forEachIndexed { index, label ->
                        Tab(
                            selected = state.selectedDayIndex == index,
                            onClick = { viewModel.selectDay(index) },
                            text = { Text(label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            val dayIndex = state.selectedDayIndex
            val daySlotMap = state.daySlots[dayIndex] ?: emptyMap()

            MealSlot.entries.forEach { slot ->
                val foods = daySlotMap[slot] ?: emptyList()
                item(key = "${dayIndex}_${slot.name}") {
                    DraftSlotSection(
                        slot = slot,
                        foods = foods,
                        onAddFood = { onNavigateToFoodSearch(dayIndex, slot) },
                        onRemoveFood = { foodId -> viewModel.removeFoodFromSlot(dayIndex, slot, foodId) }
                    )
                }
            }

            if (state.noFoodError) {
                item {
                    Text(
                        text = stringResource(R.string.create_plan_no_food_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanFormHeader(
    state: CreatePlanState,
    viewModel: CreateMealPlanViewModel
) {
    var cuisineExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text(stringResource(R.string.create_plan_name_label)) },
            placeholder = { Text(stringResource(R.string.create_plan_name_hint)) },
            isError = state.nameError,
            supportingText = if (state.nameError) {
                { Text(stringResource(R.string.create_plan_name_error), color = MaterialTheme.colorScheme.error) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::setDescription,
            label = { Text(stringResource(R.string.create_plan_desc_label)) },
            placeholder = { Text(stringResource(R.string.create_plan_desc_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = cuisineExpanded,
            onExpandedChange = { cuisineExpanded = it }
        ) {
            OutlinedTextField(
                value = state.cuisineType,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.create_plan_cuisine_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cuisineExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = cuisineExpanded,
                onDismissRequest = { cuisineExpanded = false }
            ) {
                CUISINE_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.setCuisineType(option)
                            cuisineExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftSlotSection(
    slot: MealSlot,
    foods: List<DraftSlotFood>,
    onAddFood: () -> Unit,
    onRemoveFood: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
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
                    text = slot.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = slot.timeHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                if (foods.isEmpty()) {
                    Text(
                        text = stringResource(R.string.create_plan_slot_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    foods.forEach { food ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = food.foodName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(
                                        R.string.meal_plan_serving,
                                        food.servingMultiplier
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onRemoveFood(food.foodItemId) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.create_plan_remove_food),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                TextButton(
                    onClick = onAddFood,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.create_plan_add_food),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun CreateMealPlanScreenPreview() {
    RepsTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.screen_create_meal_plan),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
