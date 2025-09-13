package com.tiramisu.groupapplication.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.tiramisu.groupapplication.domain.model.InspectionAttachment
import com.tiramisu.groupapplication.ui.component.AttachmentImage
import com.tiramisu.groupapplication.ui.component.NavBar
import com.tiramisu.groupapplication.ui.viewmodel.InspectionFormData
import com.tiramisu.groupapplication.ui.viewmodel.InspectionFormViewModel
import com.tiramisu.groupapplication.ui.viewmodel.ui_state.AttachmentUi
import com.tiramisu.groupapplication.util.map.awaitBestLocation
import kotlinx.coroutines.launch

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
            } catch (_: SecurityException) {
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
                is InspectionFormViewModel.UiEvent.Error -> {}
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
                .padding(padding),
            onGetLocation = { getCurrentLocation() },
            formData = formData,
            onSetType = viewModel::setType,
            onSetTitle = viewModel::setTitle,
            onSetNotes = viewModel::setNotes,
            onSetScore = viewModel::setScore,
            onSetOutcome = viewModel::setOutcome,
            attachments = formData.attachments,
            onAddImages = { uris -> viewModel.addAttachments(uris) },
            onDeleteAttachment = { id -> viewModel.removeAttachment(id) }
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
    attachments: List<AttachmentUi>,
    onAddImages: (List<Uri>) -> Unit,
    onDeleteAttachment: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            OutlinedTextField(
                value = formData.type,
                onValueChange = onSetType,
                label = { Text("Type") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = formData.title,
                onValueChange = onSetTitle,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = formData.notes,
                onValueChange = onSetNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 3
            )
        }

        item {
            OutlinedTextField(
                value = formData.score,
                onValueChange = onSetScore,
                label = { Text("Score") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        item {
            OutlinedTextField(
                value = formData.outcome,
                onValueChange = onSetOutcome,
                label = { Text("Outcome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
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
        }

        item {
            FilledTonalButton(
                onClick = onGetLocation,
            ) {
                Text("Pick current location")

                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = "Pick current location"
                )
            }
        }

        if (formData.generalError != null) {
            item {
                Text(text = formData.generalError, color = Color.Red)
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            AttachmentsSection(
                attachments = attachments,
                onAddImages = onAddImages,
                onDeleteAttachment = onDeleteAttachment
            )
        }
    }
}

@Composable
private fun AttachmentsSection(
    attachments: List<AttachmentUi>,
    onAddImages: (List<Uri>) -> Unit,
    onDeleteAttachment: (String) -> Unit
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) onAddImages(uris)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Attachments",
                style = MaterialTheme.typography.titleMedium
            )
            FilledTonalButton(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add images")
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Add")
            }
        }

        // Empty state
        if (attachments.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No attachments yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.padding(6.dp))
                Text(
                    text = "Use Add to select one or more images.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            AttachmentsGrid(
                attachments = attachments,
                onDeleteAttachment = onDeleteAttachment
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AttachmentsGrid(
    attachments: List<AttachmentUi>,
    onDeleteAttachment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridPadding = 8.dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            bottom = 12.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(gridPadding),
        verticalArrangement = Arrangement.spacedBy(gridPadding),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp, max = 360.dp)
    ) {
        items(
            items = attachments,
            key = { it.id }
        ) { attachment ->
            AttachmentTile(
                attachment = attachment,
                onDelete = { onDeleteAttachment(attachment.id) },
                modifier = Modifier
                    .aspectRatio(1f)
            )
        }
    }
}

@Composable
private fun AttachmentTile(
    attachment: AttachmentUi,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Preview
            AttachmentImage(
                model = attachment.coilModel,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )

            // Gradient for legibility
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f)
                            )
                        )
                    )
            )

            // Status badge (bottom-start)
            AttachmentStatusBadge(
                attachment = attachment.raw,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            )

            // Delete button (top-end)
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Remove attachment",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AttachmentStatusBadge(
    attachment: InspectionAttachment,
    modifier: Modifier = Modifier
) {
    val isError = attachment.lastError != null
    val isSynced = !isError && attachment.remoteKey != null
    val isPending = !isError && !isSynced

    val (icon, label, containerColor, labelColor) = when {
        isError -> Quad(
            Icons.Rounded.Error,
            "Failed",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )

        isSynced -> Quad(
            Icons.Rounded.CheckCircle,
            "Synced",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )

        else -> Quad(
            Icons.Rounded.CloudUpload,
            "Pending",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    AssistChip(
        onClick = { /* no-op */ },
        label = {
            Text(
                text = if (isError) {
                    // Show a shortened error if available
                    attachment.lastError.take(24).let { "$label: $it" }
                } else label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = { Icon(icon, contentDescription = null) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
            leadingIconContentColor = labelColor
        ),
        modifier = modifier
    )
}

/**
 * Small tuple helper since Kotlin doesn't have Quad by default.
 */
private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)