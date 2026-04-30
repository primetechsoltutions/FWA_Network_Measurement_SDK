package com.ptsl.fwa_network_sdk.utils

/**
 * Centralized Constants for the FWA Network SDK.
 * This class eliminates magic strings across the codebase, making maintenance
 * and future updates (e.g. changing an error code) much easier and safer.
 */
object Constants {

    // Mobile Network Codes
    const val BANGLALINK_MNC = "3"

    // Technology Filter States
    const val TECH_SKIP_MNC_MISMATCH = "SKIP_MNC_MISMATCH"
    const val TECH_NON_4G_IGNORED = "NON_4G_IGNORED"

    // Measurement Statuses
    const val STATUS_SUCCESS = "Success"
    const val STATUS_FAILED = "Failed"
    
    // Measurement Results
    const val RESULT_PASS = "Pass"
    const val RESULT_FAILED = "Failed"

    //Measurement Messages
    const val Measurement_Success_Message="Your network assessment was successful."
    const val Measurement_Failed_Message="Your network assessment failed."

    //

    const val Assessment_Timeout_Message="Network assessment timed out. Please check your internet connection."
    const val Assessment_Failed_Message="Network assessment failed. Please check your internet connection."
    const val Assessment_Error_Message="An error occurred during network assessment. Please try again later."

    // Pre-Flight Validation Error Codes
    const val ERR_CODE_PERMISSION_DENIED = "FTP_PERMISSION_DENIED"
    const val ERR_CODE_GPS_DISABLED = "FTP_GPS_DISABLED"
    const val ERR_CODE_INTERNET_UNAVAILABLE = "FTP_INTERNET_UNAVAILABLE"
    const val ERR_CODE_WIFI_CONNECTED = "FTP_WIFI_CONNECTED"
    const val ERR_CODE_MOBILE_DATA_REQUIRED = "FTP_MOBILE_DATA_REQUIRED"
    const val ERR_CODE_4G_REQUIRED = "FTP_4G_REQUIRED"
    const val ERR_CODE_BANGLALINK_DATA_UNAVAILABLE = "FTP_BANGLALINK_DATA_UNAVAILABLE"
    
    // Execution Error Codes
    const val ERR_CODE_MNC_MISMATCH = "FTP_CAPTURE_MNC_MISMATCH"
    const val ERR_CODE_NOT_4G = "FTP_CAPTURE_NOT_4G"
    const val ERR_CODE_TIMEOUT = "FTP_CAPTURE_TIMEOUT"
    const val ERR_CODE_NETWORK_ERROR = "FTP_CAPTURE_NETWORK_ERROR"
    const val ERR_CODE_EXECUTION_ERROR = "FTP_CAPTURE_EXECUTION_ERROR"

    // Pre-Flight Validation Messages
    const val ERR_MSG_PERMISSION_DENIED = "Required permissions (Location or Phone State) are missing."
    const val ERR_MSG_GPS_DISABLED = "GPS is disabled. Please enable GPS to proceed."
    const val ERR_MSG_INTERNET_UNAVAILABLE = "Internet connectivity is mandatory for network assessment."
    const val ERR_MSG_WIFI_CONNECTED = "FWA Capture requires mobile data. Please disable Wi-Fi and ensure Banglalink 4G is active."
    const val ERR_MSG_MOBILE_DATA_REQUIRED = "Mobile data connection is required for FWA Capture. Please enable mobile data."
    const val ERR_MSG_4G_REQUIRED = "4G/LTE connection is required for FWA Capture. Currently not on 4G."
    const val ERR_MSG_BANGLALINK_DATA_UNAVAILABLE = "Banglalink SIM and mobile data must be enabled for FWA Capture."
}
