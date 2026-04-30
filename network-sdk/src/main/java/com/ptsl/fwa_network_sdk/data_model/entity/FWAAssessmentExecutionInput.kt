package com.ptsl.fwa_network_sdk.data_model.entity

import androidx.annotation.Keep

@Keep
data class FWAAssessmentExecutionInput(
    val msisdn: String,
    val integratedAppVersion: String,
    val sdkInitiateTimeStamp: String,
    val integratedAppEventName: String,
    val userLatitude: Double,
    val userLongitude: Double
)
