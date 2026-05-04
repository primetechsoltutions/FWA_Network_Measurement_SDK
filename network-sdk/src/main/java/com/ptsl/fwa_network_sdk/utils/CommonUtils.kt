package com.ptsl.fwa_network_sdk.utils


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
object CommonUtils {

    private const val DATE_ONLY_FORMAT = "yyyy-MM-dd"

    /** Returns only date (yyyy-MM-dd) */
    fun getCurrentDate(): String {
        return SimpleDateFormat(DATE_ONLY_FORMAT, Locale.US)
            .format(Date())
    }

    fun isGpsEnabled(context:Context?): Boolean {
        val ctx = context ?: return false
        val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
    /** Check if Phone State permission is granted */

    fun isPhoneStatePermissionGranted(context: Context?): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
    /** Check if Location permission is granted */

    fun isLocationPermissionGranted(context:Context?): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}
