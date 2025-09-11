package com.tiramisu.groupapplication.ui.component

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.ui.viewmodel.InspectionFormData
import com.tiramisu.groupapplication.ui.viewmodel.InspectionFormViewModel
import com.tiramisu.groupapplication.util.map.awaitBestLocation
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun InspectionForm(
    modifier: Modifier = Modifier,
    viewModel: InspectionFormViewModel = hiltViewModel(),
    onSaved: (String) -> Unit = {},
    onBack: () -> Unit,
    screenTitle: String = "Add Inspection",
    navController: NavHostController,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formData by viewModel.formData.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDateTimeDialog by remember { mutableStateOf(false) }
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
    val zone = remember { ZoneId.systemDefault() }

    val createdText = remember(formData.createdAtMillis) {
        val instant = Instant.ofEpochMilli(formData.createdAtMillis)
        dateTimeFormatter.format(instant.atZone(zone).toLocalDateTime())
    }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    var locating by remember { mutableStateOf(false) }

    fun getCurrentLocation() {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
            return
        }

        if (locating) {
            return
        }

        scope.launch {
            locating = true
            try {
                val loc = fusedLocationClient.awaitBestLocation()
                if (loc != null) {
                    viewModel.setLocation(loc.latitude, loc.longitude)
                } else {
                    viewModel.setGeneralError("Couldn't get your location. Try again.")
                }
                locating = false
            } catch (e: SecurityException) {
                viewModel.setGeneralError("Location permission missing.")
            } finally {
                locating = false
            }
        }

    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                is InspectionFormViewModel.UiEvent.Saved -> onSaved(evt.id)
                is InspectionFormViewModel.UiEvent.Error -> { /* optional toast/snackbar */
                }
            }
        }
    }

    //TODO: make ui for different states

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.save() },
                        enabled = !formData.saving
                    ) {
                        Text(if (formData.saving) "Saving..." else "Save")
                    }
                }
            )
        },
        bottomBar = { NavBar(navController = navController) }
    ) { padding ->
        FormContent(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            onGetLocation = { getCurrentLocation() },
            formData = formData,
            onSetType = viewModel::setType,
            onSetTitle = viewModel::setTitle,
            onSetNotes = viewModel::setNotes,
            onSetScore = viewModel::setScore,
            onSetOutcome = viewModel::setOutcome,
            onSetStatus = viewModel::setStatus,
            createdText = createdText,
            onShowDateTimeDialog = { showDateTimeDialog = true },
            onSave = { viewModel.save() }
        )

    }

    if (!permissionsState.allPermissionsGranted) {
        Button(
            onClick = { permissionsState.launchMultiplePermissionRequest() }
        ) {
            Text("Request Location Permissions")
        }
    }

    if (showDateTimeDialog) {
        DateTimePickerDialog(
            initialEpochMillis = formData.createdAtMillis,
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
private fun FormContent(
    modifier: Modifier = Modifier,
    onGetLocation: () -> Unit,
    formData: InspectionFormData,
    onSetType: (String) -> Unit,
    onSetTitle: (String) -> Unit,
    onSetNotes: (String) -> Unit,
    onSetScore: (String) -> Unit,
    onSetOutcome: (String) -> Unit,
    onSetStatus: (InspectionStatus) -> Unit,
    createdText: String = "",
    onShowDateTimeDialog: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = formData.type,
            onValueChange = onSetType,
            label = { Text("Type") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = formData.title,
            onValueChange = onSetTitle,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = formData.notes,
            onValueChange = onSetNotes,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 3
        )

        OutlinedTextField(
            value = formData.score,
            onValueChange = onSetScore,
            label = { Text("Score") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = formData.outcome,
            onValueChange = onSetOutcome,
            label = { Text("Outcome") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        StatusDropdown(
            selected = formData.status,
            onSelected = onSetStatus,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = createdText,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Created At") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = onShowDateTimeDialog) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = formData.lat?.let { String.format("%.3f", it) } ?: "No location",
                modifier = Modifier.weight(1f),
                readOnly = true,
                onValueChange = {},
                label = { Text("Latitude") },
            )

            OutlinedTextField(
                value = formData.lng?.let { String.format("%.3f", it) } ?: "No location",
                modifier = Modifier.weight(1f),
                readOnly = true,
                onValueChange = {},
                label = { Text("Longitude") },
            )
        }

        FilledTonalButton(
            onClick = onGetLocation,
        ) {
            Text("Pick current location")

            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = "Pick current location"
            )
        }

        if (formData.generalError != null) {
            Text(text = formData.generalError, color = Color.Red)
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onSave,
                enabled = !formData.saving
            ) {
                Text(if (formData.saving) "Saving..." else "Save")
            }
        }
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
    val all = remember { InspectionStatus.entries }

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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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