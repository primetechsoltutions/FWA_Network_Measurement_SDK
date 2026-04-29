package com.ptsl.fwa_network_sdk.data_model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Concrete data model for the assessment results. Using specific types (Long, Double) ensures Gson
 * does not default to Double for numeric values.
 */
@Keep
data class AssessmentResult(
    @SerializedName("assessmentId") val assessmentId: Long? = null,
    @SerializedName("networkData") val networkData: NetworkMetrics? = null,
    @SerializedName("cellInfo") val cellInfo: CellMetadata? = null,
    @SerializedName("speedPair") val speedPair: SpeedMetrics? = null,
    @SerializedName("userInfo") val userInfo: UserMetadata? = null
)

@Keep
data class NetworkMetrics(
    @SerializedName("RSRP") val RSRP: Int? = null,
    @SerializedName("SNR") val SNR: Int? = null,
    @SerializedName("RSRQ") val RSRQ: Int? = null
)

@Keep
data class CellMetadata(
    @SerializedName("cellName") val cellName: String? = null,
    @SerializedName("eNodeBName") val eNodeBName: String? = null,
    @SerializedName("nbhDlThroughputMbps") val nbhDlThroughputMbps: Double? = null,
    @SerializedName("nbhTrafficGB") val nbhTrafficGB: Double? = null
)

@Keep
data class SpeedMetrics(
    @SerializedName("ulSpeedKbps") val ulSpeedKbps: Double? = null,
    @SerializedName("dlSpeedKbps") val dlSpeedKbps: Double? = null
)

@Keep
data class UserMetadata(
    @SerializedName("deviceManufacture") val deviceManufacture: String? = null,
    @SerializedName("deviceModel") val deviceModel: String? = null,
    @SerializedName("deviceOsVersion") val deviceOsVersion: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("msisdn") val msisdn: String? = null
)