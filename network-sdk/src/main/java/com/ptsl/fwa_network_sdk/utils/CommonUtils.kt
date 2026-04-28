package com.ptsl.fwa_network_sdk.utils


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

    /** Check if GPS is enabled */
    fun isGpsEnabled(context: android.content.Context): Boolean {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
        return locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)?:false
    }
}
