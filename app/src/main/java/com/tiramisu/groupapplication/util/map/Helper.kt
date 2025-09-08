package com.tiramisu.groupapplication.util.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

// Move camera helper
suspend fun moveCameraTo(
    camera: CameraPositionState,
    target: LatLng,
    zoom: Float = 16f,
    durationMs: Int = 700
) {
    camera.animate(
        update = CameraUpdateFactory.newLatLngZoom(target, zoom),
        durationMs = durationMs
    )
}

// Get best single-shot location (last known first, otherwise one active fix)
@SuppressLint("MissingPermission")
suspend fun FusedLocationProviderClient.awaitBestLocation(): Location? {
    val last = lastLocation.awaitOrNull()
    if (last != null) return last
    val cts = CancellationTokenSource()
    val current = getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).awaitOrNull()
    return current
}

// Await-or-null for any Task<T>
suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (!cont.isCompleted) cont.resume(it) }
    addOnFailureListener { if (!cont.isCompleted) cont.resume(null) }
    addOnCanceledListener { if (!cont.isCompleted) cont.resume(null) }
    cont.invokeOnCancellation { /* no-op */ }
}

// Reverse geocode to a readable address (best-effort)
suspend fun reverseGeocode(context: Context, latLng: LatLng): String? =
    withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val list = if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            }
            list?.firstOrNull()?.getAddressLine(0)
        } catch (e: Exception) {
            Log.w("reverseGeocode", "Failed to geocode: ${e.message}")
            null
        }
    }