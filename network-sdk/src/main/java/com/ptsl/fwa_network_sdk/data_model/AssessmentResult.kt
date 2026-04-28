package com.ptsl.fwa_network_sdk.data_model

/**
 * Concrete data model for the assessment results. Using specific types (Long, Double) ensures Gson
 * does not default to Double for numeric values.
 */
data class AssessmentResult(
    val assessmentId: Long? = null,
    val networkData: NetworkMetrics? = null,
    val cellInfo: CellMetadata? = null,
    val speedPair: SpeedMetrics? = null,
    val userInfo: UserMetadata? = null
)

data class NetworkMetrics(val RSRP: Int? = null, val SNR: Int? = null, val RSRQ: Int? = null)

data class CellMetadata(
    val cellName: String? = null,
    val eNodeBName: String? = null,
    val nbhDlThroughputMbps: Double? = null,
    val nbhTrafficGB: Double? = null
)

data class SpeedMetrics(val ulSpeedKbps: Double? = null, val dlSpeedKbps: Double? = null)

data class UserMetadata(
    val deviceManufacture: String? = null,
    val deviceModel: String? = null,
    val deviceOsVersion: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val msisdn: String? = null
)
