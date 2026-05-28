package com.reps.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.reps.app.R
import com.reps.app.core.domain.model.LoggedFood

@Composable
fun ServingsEditDialog(
    entry: LoggedFood,
    onDismiss: () -> Unit,
    onSave: (entryId: Long, servings: Double) -> Unit
) {
    var servingsText by rememberSaveable(entry.entryId) {
        val initial = if (entry.servingMultiplier % 1.0 == 0.0) {
            entry.servingMultiplier.toInt().toString()
        } else {
            "%.1f".format(entry.servingMultiplier)
        }
        mutableStateOf(initial)
    }
    var validationError by rememberSaveable(entry.entryId) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.food.name) },
        text = {
            OutlinedTextField(
                value = servingsText,
                onValueChange = {
                    servingsText = it
                    validationError = false
                },
                label = { Text(stringResource(R.string.edit_servings_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = validationError,
                supportingText = if (validationError) {
                    { Text(stringResource(R.string.edit_servings_invalid)) }
                } else {
                    null
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val servings = servingsText.toDoubleOrNull()?.takeIf { it > 0 }
                    if (servings != null) {
                        onSave(entry.entryId, servings)
                    } else {
                        validationError = true
                    }
                }
            ) {
                Text(stringResource(R.string.edit_servings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.edit_servings_cancel))
            }
        }
    )
}
