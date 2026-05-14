package com.reps.app.feature.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.core.domain.model.FoodItem
import com.reps.app.ui.theme.RepsTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    onNavigateBack: () -> Unit = {},
    onAddToPlan: ((foodId: Long, foodName: String, servings: Double) -> Unit)? = null,
    viewModel: FoodDetailViewModel = hiltViewModel()
) {
    val food by viewModel.food.collectAsStateWithLifecycle()
    val multiplier by viewModel.multiplier.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val addedToLog by viewModel.addedToLog.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedMsg = stringResource(R.string.food_detail_added)

    LaunchedEffect(addedToLog) {
        if (addedToLog) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(food?.name ?: stringResource(R.string.food_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (food) {
            null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

            else -> FoodDetailContent(
                food = food!!,
                multiplier = multiplier,
                isSaving = isSaving,
                hasSlotContext = viewModel.hasSlotContext,
                slotDisplayName = viewModel.slotDisplayName,
                onMultiplierChange = viewModel::setMultiplier,
                onAddToLog = {
                    if (viewModel.hasSlotContext) {
                        viewModel.addFoodToLog()
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(addedMsg) }
                    }
                },
                onAddToPlan = onAddToPlan?.let { callback ->
                    { servings -> callback(food!!.id, food!!.name, servings) }
                },
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun FoodDetailContent(
    food: FoodItem,
    multiplier: Float,
    isSaving: Boolean,
    hasSlotContext: Boolean,
    slotDisplayName: String?,
    onMultiplierChange: (Float) -> Unit,
    onAddToLog: () -> Unit,
    onAddToPlan: ((servings: Double) -> Unit)? = null,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val m = BigDecimal(multiplier.toDouble())

    fun Double.scale() = (BigDecimal(this) * m)
        .setScale(1, RoundingMode.HALF_UP)
        .toDouble()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (food.nameLocal != null) {
            Text(
                food.nameLocal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.food_detail_total_calories, food.caloriesPerServing.scale().toInt()),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.food_detail_calories),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.food_detail_nutrition),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                MacroRow(
                    label = stringResource(R.string.food_detail_protein),
                    value = stringResource(R.string.food_detail_grams, food.proteinPerServing.scale()),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
                MacroRow(
                    label = stringResource(R.string.food_detail_carbs),
                    value = stringResource(R.string.food_detail_grams, food.carbsPerServing.scale())
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
                MacroRow(
                    label = stringResource(R.string.food_detail_fat),
                    value = stringResource(R.string.food_detail_grams, food.fatPerServing.scale())
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
                MacroRow(
                    label = stringResource(R.string.food_detail_fiber),
                    value = stringResource(R.string.food_detail_grams, food.fiberPerServing.scale()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.food_detail_servings),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.food_detail_servings_label, multiplier, food.servingDescription),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = multiplier,
            onValueChange = onMultiplierChange,
            valueRange = 0.25f..5f,
            steps = 18,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0.25×", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text("5×", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (onAddToPlan != null) {
                    onAddToPlan(multiplier.toDouble())
                    onNavigateBack()
                } else {
                    onAddToLog()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.height(20.dp).width(20.dp)
                )
            } else {
                val label = when {
                    onAddToPlan != null -> stringResource(R.string.create_plan_add_food_to_plan)
                    hasSlotContext && slotDisplayName != null -> "Add to $slotDisplayName"
                    else -> stringResource(R.string.food_detail_add_to_log)
                }
                Text(label, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MacroRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun FoodDetailScreenPreview() {
    RepsTheme { FoodDetailScreen() }
}
