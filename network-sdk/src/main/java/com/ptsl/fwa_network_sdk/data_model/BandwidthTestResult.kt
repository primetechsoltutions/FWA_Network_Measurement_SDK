package com.ptsl.fwa_network_sdk.data_model


import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlin.math.roundToInt

@Keep
data class BandwidthTestResult(
    @SerializedName("downloadSpeedKbps") val downloadSpeedKbps: Double,
    @SerializedName("uploadSpeedKbps") val uploadSpeedKbps: Double,

    // New fields (requested)
    @SerializedName("totalDownloadBytes") val totalDownloadBytes: Long,
    @SerializedName("totalUploadBytes") val totalUploadBytes: Long
) {
    val totalDownloadMB: Double
        get() = round2(totalDownloadBytes / (1024.0 * 1024.0))

    val totalUploadMB: Double
        get() = round2(totalUploadBytes / (1024.0 * 1024.0))

    private fun round2(value: Double): Double {
        return (value * 100).roundToInt() / 100.0
    }
}
