package com.tiramisu.groupapplication.ui.screens.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.ui.viewmodel.InspectionFormViewModel
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionForm(
    modifier: Modifier = Modifier,
    viewModel: InspectionFormViewModel = hiltViewModel(),
    onSaved: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showDateTimeDialog by remember { mutableStateOf(false) }
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
    val zone = remember { ZoneId.systemDefault() }

    val createdText = remember(state.createdAtMillis) {
        val instant = Instant.ofEpochMilli(state.createdAtMillis)
        dateTimeFormatter.format(instant.atZone(zone).toLocalDateTime())
    }

    LaunchedEffect(state.savedId) {
        val id = state.savedId ?: return@LaunchedEffect
        onSaved(id)
        viewModel.clearSavedFlag()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        OutlinedTextField(
            value = state.type,
            onValueChange = viewModel::setType,
            label = { Text("Type") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::setTitle,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::setNotes,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 3
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.score,
            onValueChange = viewModel::setScore,
            label = { Text("Score") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.outcome,
            onValueChange = viewModel::setOutcome,
            label = { Text("Outcome") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        StatusDropdown(
            selected = state.status,
            onSelected = viewModel::setStatus,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = createdText,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Created At") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDateTimeDialog = true }) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        if (state.generalError != null) {
            Text(text = state.generalError ?: "", color = androidx.compose.ui.graphics.Color.Red)
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { viewModel.save() },
                enabled = !state.saving
            ) {
                Text(if (state.saving) "Saving..." else "Save")
            }
        }
    }

    if (showDateTimeDialog) {
        DateTimePickerDialog(
            initialEpochMillis = state.createdAtMillis,
            onConfirm = {
                viewModel.setDateTime(it)
                showDateTimeDialog = false
            },
            onDismiss = { showDateTimeDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    selected: InspectionStatus,
    onSelected: (InspectionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val all = remember { InspectionStatus.values().toList() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            all.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.name) },
                    onClick = {
                        onSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(
    initialEpochMillis: Long,
    onConfirm: (epochMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val initialInstant = remember(initialEpochMillis) { Instant.ofEpochMilli(initialEpochMillis) }
    val initialLocalDate = remember(initialInstant) { initialInstant.atZone(zone).toLocalDate() }
    val initialLocalTime = remember(initialInstant) { initialInstant.atZone(zone).toLocalTime() }

    // Date state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialLocalDate
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    )

    // Time state (24h format by default)
    val timePickerState = rememberTimePickerState(
        initialHour = initialLocalTime.hour,
        initialMinute = initialLocalTime.minute,
        is24Hour = true
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis
                    if (selectedDateMillis == null) {
                        onDismiss()
                        return@TextButton
                    }
                    val pickedDate = Instant.ofEpochMilli(selectedDateMillis)
                        .atZone(zone)
                        .toLocalDate()
                    val pickedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    val epoch = pickedDate
                        .atTime(pickedTime)
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()
                    onConfirm(epoch)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        // Custom content inside DatePickerDialog: DatePicker + TimePicker
        Column(Modifier.padding(8.dp)) {
            DatePicker(state = datePickerState)
            Spacer(Modifier.height(8.dp))
            TimePicker(state = timePickerState)
        }
    }
}