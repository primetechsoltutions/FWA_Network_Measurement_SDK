package com.ptsl.fwa_network_sdk.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.ptsl.fwa_network_sdk.utils.CommonUtils
import com.ptsl.fwa_network_sdk.utils.Constants

/**
 * Splitting the large NetworkStateProvider into smaller, focused interfaces.
 * This ensures that a class only needing Location info isn't forced to depend on Internet checks.
 */
interface NetworkConnectivityProvider {
    fun isInternetAvailable(): Boolean
    fun isMobileNetworkConnected(): Boolean
    fun isWifiConnected(): Boolean
    fun is4GConnected(): Boolean
}

interface SimOperatorProvider {
    fun isBanglalinkDataEnabled(): Boolean
    fun getActiveNetworkMNC(): String
    fun isPhoneStatePermissionGranted(): Boolean

}

interface LocationStateProvider {
    fun isGpsEnabled(): Boolean
    fun hasLocationPermissions(): Boolean
}

/**
 * Composite interface combining the segregated interfaces. 
 * This maintains backward compatibility so existing code using NetworkStateProvider doesn't break.
 */
interface NetworkStateProvider : NetworkConnectivityProvider, SimOperatorProvider, LocationStateProvider

internal class NetworkStateProviderImpl(private val context: Context) : NetworkStateProvider {
    companion object {
        private const val BL_SIM = Constants.BANGLALINK_MNC
    }

    override fun isInternetAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: Exception) {
            false
        }
    }

    override fun isMobileNetworkConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (_: Exception) {
            false
        }
    }

    override fun isWifiConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun is4GConnected(): Boolean {
        return try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val networkType = if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                telephonyManager?.dataNetworkType
            } else {
                TelephonyManager.NETWORK_TYPE_UNKNOWN
            }
            networkType == TelephonyManager.NETWORK_TYPE_LTE
        } catch (_: Exception) {
            false
        }
    }

    override fun isBanglalinkDataEnabled(): Boolean {
        return try {
            val sm = SubscriptionManager.from(context)
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }

            val subscriptions = sm.activeSubscriptionInfoList
            if (subscriptions.isNullOrEmpty()) return false

            val hasBLSim = subscriptions.any { it.mnc.toString().removePrefix("0") == BL_SIM }
            if (!hasBLSim) return false

            val defaultDataId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SubscriptionManager.getDefaultDataSubscriptionId()
            } else {
                -1
            }

            if (defaultDataId != -1) {
                val info = sm.getActiveSubscriptionInfo(defaultDataId)
                info?.mnc?.toString()?.removePrefix("0") == BL_SIM
            } else {
                subscriptions.firstOrNull()?.mnc?.toString()?.removePrefix("0") == BL_SIM
            }
        } catch (_: Exception) {
            false
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    override fun getActiveNetworkMNC(): String {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val sm = SubscriptionManager.from(context)
                val dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                if (dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    val si = sm.getActiveSubscriptionInfo(dataSubId)
                    return "0${si?.mnc ?: -1}"
                }
            }
        } catch (_: Exception) {
        }
        return "0-1"
    }

    override fun isGpsEnabled(): Boolean {
        return CommonUtils.isGpsEnabled(context)
    }

    override fun hasLocationPermissions(): Boolean {
        return CommonUtils.isLocationPermissionGranted(context)
    }

    override fun isPhoneStatePermissionGranted(): Boolean {
        return CommonUtils.isPhoneStatePermissionGranted(context)
    }
}
