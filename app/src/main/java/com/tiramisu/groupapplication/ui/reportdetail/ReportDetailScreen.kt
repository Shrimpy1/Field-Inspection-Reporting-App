package com.tiramisu.groupapplication.ui.reportdetail

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    onSaveComplete: () -> Unit,
    viewModel: ReportDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Launcher for location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                getCurrentLocation(context) { lat, lon ->
                    viewModel.latitude = lat
                    viewModel.longitude = lon
                }
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Launcher for the photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris -> viewModel.imageUris = uris }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("New Inspection Report") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.reportTitle,
                onValueChange = { viewModel.reportTitle = it },
                label = { Text("Report Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.reportNotes,
                onValueChange = { viewModel.reportNotes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            // --- Image Attachment Section ---
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Attach Images")
            }
            if (viewModel.imageUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.imageUris) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Attached image",
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // --- Location Section ---
            OutlinedButton(
                onClick = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Capture GPS Location")
            }
            if (viewModel.latitude != null) {
                Text("Location: ${viewModel.latitude}, ${viewModel.longitude}")
            }

            Spacer(Modifier.weight(1f))

            // --- Save Button ---
            Button(
                onClick = {
                    viewModel.saveReport()
                    onSaveComplete()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Save Report")
            }
        }
    }
}

private fun getCurrentLocation(context: Context, onResult: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onResult(location.latitude, location.longitude)
            } else {
                Toast.makeText(context, "Could not retrieve location.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to get location.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Location permission error.", Toast.LENGTH_SHORT).show()
    }
}
