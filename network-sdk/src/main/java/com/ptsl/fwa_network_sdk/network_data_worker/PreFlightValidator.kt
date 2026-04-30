package com.ptsl.fwa_network_sdk.network_data_worker

import com.ptsl.fwa_network_sdk.provider.NetworkStateProvider
import com.ptsl.fwa_network_sdk.utils.Constants

/**
 * Encapsulates the result of a Pre-Flight validations.
 */
data class PreFlightResult(
    val message: String, val errorCode: String, val statusCode: Int
)

interface EligibilityRule {
    fun evaluate(provider: NetworkStateProvider): PreFlightResult?
}

object PreFlightValidator {

    private val rules = listOf(
        object : EligibilityRule {
            override fun evaluate(provider: NetworkStateProvider) =
                if (!provider.hasLocationPermissions()) PreFlightResult(
                    Constants.ERR_MSG_PERMISSION_DENIED, Constants.ERR_CODE_PERMISSION_DENIED, 400
                ) else null
        },
        object : EligibilityRule {
            override fun evaluate(provider: NetworkStateProvider) =
                if (!provider.isGpsEnabled()) PreFlightResult(
                    Constants.ERR_MSG_GPS_DISABLED, Constants.ERR_CODE_GPS_DISABLED, 400
                ) else null
        },
        object : EligibilityRule {
            override fun evaluate(provider: NetworkStateProvider) =
                if (!provider.isInternetAvailable()) PreFlightResult(
                    Constants.ERR_MSG_INTERNET_UNAVAILABLE, Constants.ERR_CODE_INTERNET_UNAVAILABLE, 400
                ) else null
        },
        object : EligibilityRule {
            override fun evaluate(provider: NetworkStateProvider) =
                if (provider.isWifiConnected()) PreFlightResult(
                    Constants.ERR_MSG_WIFI_CONNECTED, Constants.ERR_CODE_WIFI_CONNECTED, 400
                ) else null
        },
        object : EligibilityRule {
            override fun evaluate(provider: NetworkStateProvider) =
                if (!provider.isMobileNetworkConnected()) PreFlightResult(
                    Constants.ERR_MSG_MOBILE_DATA_REQUIRED, Constants.ERR_CODE_MOBILE_DATA_REQUIRED, 400
                ) else null
        },
        object : EligibilityRule {
            override fun evaluate(provider: NetworkStateProvider) =
                if (!provider.is4GConnected()) PreFlightResult(
                    Constants.ERR_MSG_4G_REQUIRED, Constants.ERR_CODE_4G_REQUIRED, 400
                ) else null
        },
        object : EligibilityRule {
            override fun evaluate(provider: NetworkStateProvider) =
                if (!provider.isBanglalinkDataEnabled()) PreFlightResult(
                    Constants.ERR_MSG_BANGLALINK_DATA_UNAVAILABLE, Constants.ERR_CODE_BANGLALINK_DATA_UNAVAILABLE, 400
                ) else null
        }
    )

    fun validate(provider: NetworkStateProvider): PreFlightResult? {
        for (rule in rules) {
            val result = rule.evaluate(provider)
            if (result != null) return result
        }
        return null
    }
}
