package com.tiramisu.groupapplication.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.tiramisu.groupapplication.ui.viewmodel.InspectionPin
import com.tiramisu.groupapplication.ui.viewmodel.InspectionsLocState
import com.tiramisu.groupapplication.ui.viewmodel.InspectionsMapViewModel
import com.tiramisu.groupapplication.util.map.awaitBestLocation
import com.tiramisu.groupapplication.util.map.moveCameraTo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionsMapScreen(
    viewModel: InspectionsMapViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateInspection: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        "All Inspection Locations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is InspectionsLocState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is InspectionsLocState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                is InspectionsLocState.Success -> {
                    AllLocationsMap(
                        pins = state.pins,
                        onNavigateInspection = onNavigateInspection
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, MapsComposeExperimentalApi::class)
@SuppressLint("MissingPermission")
@Composable
private fun AllLocationsMap(
    pins: List<InspectionPin>,
    onNavigateInspection: (String) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val permissionsState: MultiplePermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val cameraPositionState = rememberCameraPositionState()
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var moveToUserRequested by remember { mutableStateOf(true) }
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            fusedLocationClient.awaitBestLocation()?.let {
                currentLocation = LatLng(it.latitude, it.longitude)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = permissionsState.allPermissionsGranted
            ),
            onMapLoaded = { mapLoaded = true }
        ) {
            MapEffect(pins, mapLoaded) { map ->
                if (mapLoaded && pins.isNotEmpty()) {
                    val builder = LatLngBounds.Builder()
                    pins.forEach { builder.include(LatLng(it.lat, it.lng)) }
                    val bounds = builder.build()
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds, 120)
                    )

                    moveToUserRequested = false
                }
            }

            pins.forEach { pin ->
                Marker(
                    state = MarkerState(position = LatLng(pin.lat, pin.lng)),
                    title = pin.title ?: "Inspection",
                    snippet = "${pin.lat}, ${pin.lng}",
                    onInfoWindowClick = { onNavigateInspection(pin.id) }
                )
            }
        }

        LaunchedEffect(mapLoaded, currentLocation, moveToUserRequested) {
            if (mapLoaded && moveToUserRequested && currentLocation != null) {
                moveCameraTo(cameraPositionState, currentLocation!!)
                moveToUserRequested = false
            }
        }

        Button(
            onClick = {
                moveToUserRequested = true
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        ) {
            Text("Move to current Location")
        }
    }
}