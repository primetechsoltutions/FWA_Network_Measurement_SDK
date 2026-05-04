package com.ptsl.fwa_network_sdk.network_data_worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.ptsl.fwa_network_sdk.utils.CommonUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Utility to fetch current location using Play Services.
 * Returns a fallback (0.0, 0.0) if permissions are missing or timeout occurs.
 */
object LocationHelper {
    /**
     * Attempts to fetch current location coordinate with a 5-second timeout.
     */
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double> {
        val hasPermission = CommonUtils.isLocationPermissionGranted(context)
        if (!hasPermission) return Pair(0.0, 0.0)

        val client = LocationServices.getFusedLocationProviderClient(context)

        try {
            // 1. Try to get a fresh location with a 10-second timeout
            val location = withTimeoutOrNull(10000) {
                suspendCancellableCoroutine { cont ->
                    val cts = CancellationTokenSource()
                    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { loc ->
                            cont.resume(loc) {}
                        }.addOnFailureListener {
                            cont.resume(null) {}
                        }
                    cont.invokeOnCancellation {
                        cts.cancel()
                    }
                }
            }

            if (location != null) {
                return Pair(location.latitude, location.longitude)
            }

            // 2. Fallback to last known location if fresh location fails or times out
            return suspendCancellableCoroutine { cont ->
                client.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        cont.resume(Pair(lastLoc.latitude, lastLoc.longitude)) {}
                    } else {
                        cont.resume(Pair(0.0, 0.0)) {}
                    }
                }.addOnFailureListener {
                    cont.resume(Pair(0.0, 0.0)) {}
                }
            }

        } catch (_: Exception) {
            return Pair(0.0, 0.0)
        }
    }
}