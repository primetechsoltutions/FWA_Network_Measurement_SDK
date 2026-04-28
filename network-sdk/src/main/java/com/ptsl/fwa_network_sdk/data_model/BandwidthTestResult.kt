package com.ptsl.fwa_network_sdk.data_model

import kotlin.math.roundToInt

data class BandwidthTestResult(
    val downloadSpeedKbps: Double,
    val uploadSpeedKbps: Double,

    // New fields (requested)
    val totalDownloadBytes: Long,
    val totalUploadBytes: Long
) {
    val totalDownloadMB: Double
        get() = round2(totalDownloadBytes / (1024.0 * 1024.0))

    val totalUploadMB: Double
        get() = round2(totalUploadBytes / (1024.0 * 1024.0))

    private fun round2(value: Double): Double {
        return (value * 100).roundToInt() / 100.0
    }
}
