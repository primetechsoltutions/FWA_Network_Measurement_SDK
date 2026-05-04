package com.ptsl.fwa_network_sdk.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

/**
 * Handles permission requests and GPS enablement prompts for the Network SDK.
 * Implements case-by-case logic for Standard (once-per-day) vs FTP (every-time) GPS prompts.
 */
class CheckPermissionHandler private constructor(
    private val activity: AppCompatActivity?, private val fragment: Fragment?
) {

    constructor(activity: AppCompatActivity) : this(activity, null)
    constructor(fragment: Fragment) : this(null, fragment)

    private val caller: ActivityResultCaller? = activity ?: fragment

    private val safeContext: Context?
        get() = activity ?: fragment?.context

    private var isRequestInProgress = false
    private var permissionCallback: ((Map<String, Boolean>) -> Unit)? = null
    private var gpsCallback: ((Boolean) -> Unit)? = null

    private val permissionLauncher = caller?.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionCallback?.invoke(it)
    }

    private val gpsResolutionLauncher = caller?.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        gpsCallback?.invoke(result.resultCode == Activity.RESULT_OK)
    }

    private fun canLaunchUi(): Boolean {
        activity?.let { return !it.isFinishing && !it.isDestroyed }
        fragment?.let {
            val act = it.activity ?: return false
            return it.isAdded && !act.isFinishing && !act.isDestroyed
        }
        return false
    }

    fun isPermissionGranted(): Boolean {
        return isAllPermissionsGrantedExcludingGps() && CommonUtils.isGpsEnabled(safeContext)
    }

    fun isAllPermissionsGrantedExcludingGps(): Boolean {
        return CommonUtils.isPhoneStatePermissionGranted(safeContext) && CommonUtils.isLocationPermissionGranted(
            safeContext
        )
    }

    fun requestPermission(callback: (Boolean) -> Unit) {
        if (isPermissionGranted()) {
            callback(true)
            return
        }
        // Simple in-flight guard: if a permission/GPS resolver flow is already being shown,
        // ignore any new request (do not queue callbacks).
        if (isRequestInProgress) return
        isRequestInProgress = true


        // GPS Prompt Logic
        if (isAllPermissionsGrantedExcludingGps() && !CommonUtils.isGpsEnabled(safeContext)) {
            checkAndResolveLocationSettings(callback)
            return
        }

        startPermissionFlow(callback)
    }

    private fun startPermissionFlow(callback: (Boolean) -> Unit) {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        requestPermissions(permissions) { _ ->
            val allGranted = isAllPermissionsGrantedExcludingGps()
            if (allGranted && !CommonUtils.isGpsEnabled(safeContext)) {
                checkAndResolveLocationSettings(callback)
            } else if (!allGranted) {
                finishRequest(callback)
            } else {
                finishRequest(callback)
            }
        }
    }

    private fun requestPermissions(
        permissions: Array<String>, callback: (Map<String, Boolean>) -> Unit
    ) {
        if (!canLaunchUi()) {
            // Can't show dialogs; reset state and return current result.
            resetInFlightFlagOnly()
            callback(emptyMap())
            return
        }
        permissionCallback = callback
        permissionLauncher?.launch(permissions)
    }

    private fun resolveGps(intentSenderRequest: IntentSenderRequest, callback: (Boolean) -> Unit) {
        if (!canLaunchUi()) {
            resetInFlightFlagOnly()
            callback(false)
            return
        }
        gpsCallback = callback
        gpsResolutionLauncher?.launch(intentSenderRequest)
    }

    private fun checkAndResolveLocationSettings(callback: (Boolean) -> Unit) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, System.currentTimeMillis() % 10000
        ).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val ctx = safeContext ?: run {
            finishRequest(callback)
            return
        }

        LocationServices.getSettingsClient(ctx).checkLocationSettings(builder.build())
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val exception = task.exception
                    if (exception is ResolvableApiException) {
                        Log.d(
                            "CheckPermissionHandler",
                            "Resolution required for GPS. Status: ${exception.statusCode}"
                        )
                        try {
                            val intentSenderRequest =
                                IntentSenderRequest.Builder(exception.resolution.intentSender)
                                    .build()
                            resolveGps(intentSenderRequest) { isSuccess ->
                                if (isSuccess) {
                                    android.os.Handler(android.os.Looper.getMainLooper())
                                        .postDelayed({
                                            Log.d(
                                                "CheckPermissionHandler",
                                                "GPS resolution successful, finishing request."
                                            )
                                            finishRequest(callback)
                                        }, 1000)
                                } else {
                                    Log.d(
                                        "CheckPermissionHandler",
                                        "GPS resolution cancelled or failed."
                                    )
                                    finishRequest(callback)
                                }
                            }
                        } catch (e: Exception) {
                            // Clean Code: Do not swallow exceptions silently. Log them so production
                            // bugs or crashes related to GPS intent resolution can be traced.
                            Log.e(
                                "CheckPermissionHandler",
                                "Failed to launch GPS resolution: ${e.message}"
                            )
                            finishRequest(callback)
                        }
                    } else {
                        finishRequest(callback)
                    }
                } else {
                    finishRequest(callback)
                }
            }
    }

    private fun finishRequest(callback: (Boolean) -> Unit) {
        val result = isPermissionGranted()
        isRequestInProgress = false
        callback(result)
    }

    private fun resetInFlightFlagOnly() {
        isRequestInProgress = false
    }
}
