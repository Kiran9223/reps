package com.reps.app.feature.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.ui.components.AiErrorInline
import com.reps.app.ui.components.resolveAiErrorMessage
import com.reps.app.ui.theme.RepsTheme

private val CUISINE_TAG_LABELS = listOf(
    "south-indian" to "South Indian",
    "north-indian" to "North Indian",
    "mediterranean" to "Mediterranean",
    "continental" to "Continental",
    "chinese" to "Chinese"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomFoodCreationScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: CustomFoodCreationViewModel = hiltViewModel()
) {
    val state by viewModel.formState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMsg = stringResource(R.string.custom_food_saved)

    LaunchedEffect(state.savedFoodId) {
        if (state.savedFoodId != null) {
            snackbarHostState.showSnackbar(savedMsg)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.custom_food_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            RepsTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.custom_food_name_label),
                placeholder = stringResource(R.string.custom_food_name_placeholder)
            )
            Spacer(Modifier.height(12.dp))

            RepsTextField(
                value = state.servingDescription,
                onValueChange = viewModel::onServingDescChange,
                label = stringResource(R.string.custom_food_serving_desc_label),
                placeholder = stringResource(R.string.custom_food_serving_desc_placeholder)
            )
            Spacer(Modifier.height(12.dp))

            RepsTextField(
                value = state.servingGrams,
                onValueChange = viewModel::onServingGramsChange,
                label = stringResource(R.string.custom_food_serving_grams_label),
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.custom_food_nutrition_heading),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ElevatedButton(
                    onClick = viewModel::estimateNutrition,
                    enabled = state.name.isNotBlank() && !state.isEstimating && !state.isSubmitting,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (state.isEstimating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.custom_food_estimating),
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.custom_food_estimate_ai),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            RepsTextField(
                value = state.calories,
                onValueChange = viewModel::onCaloriesChange,
                label = stringResource(R.string.custom_food_calories_label),
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(12.dp))

            RepsTextField(
                value = state.protein,
                onValueChange = viewModel::onProteinChange,
                label = stringResource(R.string.custom_food_protein_label),
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(12.dp))

            RepsTextField(
                value = state.carbs,
                onValueChange = viewModel::onCarbsChange,
                label = stringResource(R.string.custom_food_carbs_label),
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(12.dp))

            RepsTextField(
                value = state.fat,
                onValueChange = viewModel::onFatChange,
                label = stringResource(R.string.custom_food_fat_label),
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(12.dp))

            RepsTextField(
                value = state.fiber,
                onValueChange = viewModel::onFiberChange,
                label = stringResource(R.string.custom_food_fiber_label),
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.custom_food_cuisine_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CUISINE_TAG_LABELS.forEach { (tag, label) ->
                    FilterChip(
                        selected = tag in state.selectedCuisineTags,
                        onClick = { viewModel.onCuisineTagToggle(tag) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            state.errorMessage?.let { errorKey ->
                Spacer(Modifier.height(8.dp))
                AiErrorInline(
                    message = resolveAiErrorMessage(errorKey),
                    onRetry = viewModel::estimateNutrition
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::saveFood,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting && !state.isEstimating,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text(
                        stringResource(R.string.custom_food_save),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RepsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun CustomFoodCreationScreenPreview() {
    RepsTheme { CustomFoodCreationScreen() }
}
