package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.ui.component.AttachmentImage
import com.tiramisu.groupapplication.ui.component.NavBar
import com.tiramisu.groupapplication.ui.viewmodel.InspectionDetailsEvent
import com.tiramisu.groupapplication.ui.viewmodel.InspectionDetailsState
import com.tiramisu.groupapplication.ui.viewmodel.InspectionDetailsViewModel
import com.tiramisu.groupapplication.ui.viewmodel.ui_state.AttachmentUi
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
    val uid = state.userId
    val isEditingAllowed =
        inspection?.status == InspectionStatus.PENDING_REVIEW && inspection.createdBy == uid

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        inspection?.title ?: "Inspection Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
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
                        enabled = isEditingAllowed
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = isEditingAllowed && !state.deleteInProgress
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
                        inspection = inspection,
                        attachments = state.attachmentItems
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectionDetailsContent(
    modifier: Modifier = Modifier,
    inspection: Inspection,
    attachments: List<AttachmentUi>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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

        if (attachments.isNotEmpty()) {
            item {
                AttachmentsGallery(
                    attachments = attachments,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }

        val items = listOf(
            "Created" to formatInstant(inspection.createdAt),
            "Type" to inspection.type.display,
            "Status" to inspection.status.name,
            "Score" to inspection.score,
            "Outcome" to inspection.outcome,
            "Location" to when {
                inspection.lat != null && inspection.lng != null -> "${inspection.lat}, ${inspection.lng}"
                else -> "Not set"
            },
            "Notes" to inspection.notes
        )

        itemsIndexed(
            items = items,
            key = { _, pair -> pair.first }
        ) { index, (label, value) ->
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = value, style = MaterialTheme.typography.bodyLarge)
            }
            if (index < items.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AttachmentsGallery(
    attachments: List<AttachmentUi>,
    modifier: Modifier = Modifier,
    onImageClick: (index: Int) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val config = LocalConfiguration.current
    val itemWidth =
        remember(config) { (config.screenWidthDp.dp - 32.dp) } // full width minus horizontal padding
    val itemHeight = 220.dp

    Box(modifier = modifier) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
        ) {
            itemsIndexed(attachments) { index, item ->
                AttachmentImage(
                    model = item.coilModel,
                    modifier = Modifier
                        .width(itemWidth)
                        .height(itemHeight)
                    // If you want click-to-preview later:
                    // .clickable { onImageClick(index) }
                )
            }
        }

        val currentIndex by remember {
            derivedStateOf {
                (listState.firstVisibleItemIndex + 1)
                    .coerceAtMost(attachments.size.coerceAtLeast(1))
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text(
                text = "$currentIndex/${attachments.size}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
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