package com.reps.app.feature.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reps.app.R
import com.reps.app.feature.onboarding.ActivityLevel
import com.reps.app.feature.onboarding.OnboardingViewModel
import com.reps.app.ui.components.PrivacyNoticeBanner
import com.reps.app.ui.components.profileValidationErrorMessage
import com.reps.app.ui.theme.RepsTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dietaryLabels = stringArrayResource(R.array.dietary_restriction_labels)
    val cuisineLabels = stringArrayResource(R.array.cuisine_preference_labels)
    val activityLabels = stringArrayResource(R.array.activity_level_labels)

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDiscardDialog,
            title = { Text(stringResource(R.string.settings_unsaved_title)) },
            text = { Text(stringResource(R.string.settings_unsaved_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmDiscard()
                    viewModel.dismissDiscardDialog()
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.settings_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDiscardDialog) {
                    Text(stringResource(R.string.settings_keep_editing))
                }
            }
        )
    }

    BackHandler(enabled = state.isLoaded) {
        if (viewModel.tryNavigateBack()) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_settings)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.tryNavigateBack()) onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (!state.isLoaded) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader(stringResource(R.string.settings_section_profile)) }

            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.onboarding_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = state.age,
                    onValueChange = viewModel::onAgeChange,
                    label = { Text(stringResource(R.string.onboarding_age_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { SectionHeader(stringResource(R.string.settings_section_body)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.weightKg,
                        onValueChange = viewModel::onWeightChange,
                        label = { Text(stringResource(R.string.onboarding_weight_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.targetWeightKg,
                        onValueChange = viewModel::onTargetWeightChange,
                        label = { Text(stringResource(R.string.onboarding_target_weight_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = state.heightCm,
                    onValueChange = viewModel::onHeightChange,
                    label = { Text(stringResource(R.string.onboarding_height_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { SectionHeader(stringResource(R.string.settings_section_training)) }

            item {
                ActivityLevelDropdown(
                    selected = state.activityLevel,
                    labels = activityLabels,
                    onSelect = viewModel::onActivityLevelChange
                )
            }

            item {
                Column {
                    Text(
                        stringResource(R.string.onboarding_workout_days_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.onboarding_workout_days_value, state.workoutDaysPerWeek),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = state.workoutDaysPerWeek.toFloat(),
                        onValueChange = { viewModel.onWorkoutDaysChange(it.toInt()) },
                        valueRange = 1f..7f,
                        steps = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.onboarding_shoulder_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.hasShoulderRestriction,
                        onCheckedChange = viewModel::onShoulderChange
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.settings_section_preferences)) }

            item {
                Text(
                    stringResource(R.string.onboarding_dietary_restrictions_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OnboardingViewModel.DIETARY_RESTRICTION_KEYS.forEachIndexed { i, key ->
                        val label = dietaryLabels.getOrElse(i) { key }
                        FilterChip(
                            selected = key in state.selectedDietaryRestrictions,
                            onClick = { viewModel.onDietaryToggle(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (key in state.selectedDietaryRestrictions) {
                                { Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.onboarding_cuisine_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OnboardingViewModel.CUISINE_PREFERENCE_KEYS.forEachIndexed { i, key ->
                        val label = cuisineLabels.getOrElse(i) { key }
                        FilterChip(
                            selected = key in state.selectedCuisinePreferences,
                            onClick = { viewModel.onCuisineToggle(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (key in state.selectedCuisinePreferences) {
                                { Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.settings_section_ai)) }

            item {
                AiPreferencesSection(
                    state = state,
                    onCloudAssistChange = viewModel::setCloudAssistUserEnabled,
                    onProteinRemindersChange = viewModel::setProteinRemindersEnabled,
                    onRefreshModelStatus = viewModel::refreshModelStatus
                )
            }

            item { SectionHeader(stringResource(R.string.settings_section_privacy)) }

            item {
                PrivacyDataSection(
                    onDeviceLlmActive = state.onDeviceLlmActive,
                    cloudAssistAvailable = state.cloudAssistAvailable,
                    cloudAssistActive = state.cloudAssistActive
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                state.validationError?.let { error ->
                    Text(
                        profileValidationErrorMessage(error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                if (state.saveFailed) {
                    Text(
                        stringResource(R.string.settings_save_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                if (state.savedSuccess) {
                    Text(
                        stringResource(R.string.settings_saved),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.settings_save))
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun AiPreferencesSection(
    state: SettingsUiState,
    onCloudAssistChange: (Boolean) -> Unit,
    onProteinRemindersChange: (Boolean) -> Unit,
    onRefreshModelStatus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.cloudAssistAvailable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_cloud_assist_toggle),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.settings_cloud_assist_toggle_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.cloudAssistUserEnabled,
                    onCheckedChange = onCloudAssistChange
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_protein_reminders),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.settings_protein_reminders_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.proteinRemindersEnabled,
                onCheckedChange = onProteinRemindersChange
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.settings_model_status_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (state.onDeviceModelInstalled) {
                    stringResource(R.string.settings_model_status_found)
                } else {
                    stringResource(R.string.settings_model_status_missing)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onRefreshModelStatus) {
                Text(stringResource(R.string.settings_model_refresh))
            }
        }
    }
}

@Composable
private fun PrivacyDataSection(
    onDeviceLlmActive: Boolean,
    cloudAssistAvailable: Boolean,
    cloudAssistActive: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.settings_privacy_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PrivacyNoticeBanner(
            text = stringResource(
                if (onDeviceLlmActive) R.string.settings_privacy_on_device_active
                else R.string.settings_privacy_on_device_inactive
            )
        )
        PrivacyNoticeBanner(
            text = stringResource(
                when {
                    cloudAssistActive -> R.string.settings_privacy_cloud_on
                    cloudAssistAvailable -> R.string.settings_privacy_cloud_user_off
                    else -> R.string.settings_privacy_cloud_off
                }
            )
        )
        PrivacyDetailBlock(
            title = stringResource(R.string.settings_privacy_stays_local),
            body = stringResource(R.string.settings_privacy_stays_local_body)
        )
        PrivacyDetailBlock(
            title = stringResource(R.string.settings_privacy_on_device_tasks),
            body = stringResource(R.string.settings_privacy_on_device_tasks_body)
        )
        PrivacyDetailBlock(
            title = stringResource(R.string.settings_privacy_cloud_tasks),
            body = stringResource(R.string.settings_privacy_cloud_tasks_body)
        )
        PrivacyDetailBlock(
            title = stringResource(R.string.settings_privacy_coach_clarification_title),
            body = stringResource(R.string.settings_privacy_coach_clarification_body)
        )
        PrivacyDetailBlock(
            title = stringResource(R.string.settings_privacy_optional_network),
            body = stringResource(R.string.settings_privacy_optional_network_body)
        )
        PrivacyDetailBlock(
            title = stringResource(R.string.settings_ai_setup_title),
            body = stringResource(R.string.settings_ai_setup_on_device)
        )
        if (!cloudAssistAvailable) {
            Text(
                text = stringResource(R.string.settings_ai_setup_cloud),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyDetailBlock(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityLevelDropdown(
    selected: ActivityLevel,
    labels: Array<String>,
    onSelect: (ActivityLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val levels = ActivityLevel.entries.toList()
    val selectedIndex = levels.indexOf(selected).coerceAtLeast(0)
    val selectedLabel = labels.getOrElse(selectedIndex) { selected.name }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.onboarding_activity_level_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            levels.forEachIndexed { i, level ->
                DropdownMenuItem(
                    text = { Text(labels.getOrElse(i) { level.name }) },
                    onClick = {
                        onSelect(level)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun SettingsScreenPreview() {
    RepsTheme { SettingsScreen() }
}
