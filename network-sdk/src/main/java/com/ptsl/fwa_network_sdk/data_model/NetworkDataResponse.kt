package com.ptsl.fwa_network_sdk.data_model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class NetworkDataResponse (

    @SerializedName("status") val status: String? = null,
    @SerializedName("testResult") val testResult: String? = null,
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: AssessmentResult? = null
)