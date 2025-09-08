package com.tiramisu.groupapplication.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.tiramisu.groupapplication.ui.viewmodel.InspectionLocationViewModel
import com.tiramisu.groupapplication.ui.viewmodel.ReportLocState
import com.tiramisu.groupapplication.util.map.moveCameraTo

@Composable
fun EventLocationScreen(
    viewModel: InspectionLocationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is ReportLocState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is ReportLocState.Success -> {
            LocationMap(
                lat = state.lat,
                lng = state.lng
            )
        }

        is ReportLocState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission", "UnrememberedMutableState")
@Composable
fun LocationMap(
    lat: Double,
    lng: Double,
) {
    val permissions: MultiplePermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Optionally request location permission so we can show the user dot.
    LaunchedEffect(Unit) {
        permissions.launchMultiplePermissionRequest()
    }

    val latLng = LatLng(lat, lng)
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(permissions.allPermissionsGranted) {
        if (permissions.allPermissionsGranted) {
            moveCameraTo(cameraPositionState, latLng)
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = permissions.allPermissionsGranted
        ),
    ) {
        Marker(
            snippet = "$lat, $lng"
        )
    }
}