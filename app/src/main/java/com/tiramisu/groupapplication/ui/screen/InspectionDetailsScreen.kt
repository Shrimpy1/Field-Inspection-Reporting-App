package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.ui.component.NavBar
import com.tiramisu.groupapplication.ui.viewmodel.InspectionDetailsEvent
import com.tiramisu.groupapplication.ui.viewmodel.InspectionDetailsState
import com.tiramisu.groupapplication.ui.viewmodel.InspectionDetailsViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

@Composable
fun InspectionDetailsRoute(
    viewModel: InspectionDetailsViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is InspectionDetailsEvent.Deleted -> navController.popBackStack()
                is InspectionDetailsEvent.Error -> {
                    //TODO: implement error screen
                }
            }
        }
    }

    InspectionDetailsScreen(
        onBack = { navController.popBackStack() },
        state = state,
        onDelete = { confirmDelete = true },
        onEdit = {
            state.inspection?.id?.let {
                navController.navigate(
                    Routes.InspectionEdit.createRoute(
                        it
                    )
                )
            }
        },
        onOpenMap = {
            state.inspection?.id?.let {
                navController.navigate(
                    Routes.InspectionMap.createRoute(
                        it
                    )
                )
            }
        },
        navController = navController
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete inspection") },
            text = { Text("Are you sure you want to delete this inspection? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteInspection()
                    },
                    enabled = !state.deleteInProgress
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionDetailsScreen(
    state: InspectionDetailsState,
    onBack: () -> Unit = {},
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
    onOpenMap: () -> Unit = {},
    navController: NavHostController
) {
    val inspection = state.inspection

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(inspection?.title ?: "Inspection Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenMap,
                        enabled = inspection?.lat != null && inspection.lng != null && inspection.id.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Place, contentDescription = "Open Map")
                    }
                    IconButton(
                        onClick = onEdit,
                        enabled = inspection?.id?.isNotBlank() == true
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = inspection != null && !state.deleteInProgress
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        bottomBar = {
            NavBar(navController = navController)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                inspection == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Inspection not found")
                    }
                }

                else -> {
                    InspectionDetailsContent(
                        inspection = inspection
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectionDetailsContent(
    modifier: Modifier = Modifier,
    inspection: Inspection
) {
    val items = buildList<Pair<String, String>> {
//        add("Title" to inspection.title)
        add("Created" to formatInstant(inspection.createdAt))
        add("Type" to inspection.type)
        add("Status" to inspection.status.name)
        add("Score" to inspection.score)
        add("Outcome" to inspection.outcome)
        add(
            "Location" to when {
                inspection.lat != null && inspection.lng != null -> "${inspection.lat}, ${inspection.lng}"
                else -> "Not set"
            }
        )
        add("Notes" to inspection.notes)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (inspection.pendingSync) {
            item {
                PendingSyncInfoBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }


        items(items) { (label, value) ->
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = value, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PendingSyncInfoBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = null
            )
            Text(
                text = "This item is not synced yet — waiting for a connection to push to the server.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}


private fun formatInstant(instant: Instant): String {
    val zoned = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(zoned)
}