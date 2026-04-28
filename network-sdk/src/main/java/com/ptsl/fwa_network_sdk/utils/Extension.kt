package com.ptsl.fwa_network_sdk.utils

import android.os.Build
import com.ptsl.fwa_network_sdk.data_model.entity.FTPNetworkDataEntity
import com.ptsl.fwa_network_sdk.dl_ul_test.DownloadUploadHelper
import cz.mroczis.netmonster.core.model.cell.*
import kotlinx.coroutines.ensureActive
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.coroutines.coroutineContext

suspend fun ICell.prepareFTPData(
    locationPair: Pair<Double, Double>,
    downloader: DownloadUploadHelper,
    hasMobileInternet: Boolean = false,
    activeNetworkMnc: String = "-1",
): FTPNetworkDataEntity {
    val mcc = this.network?.mcc
    val mnc = this.network?.mnc
    
    val type = when (this) {
        is CellGsm -> "2G"
        is CellWcdma, is CellTdscdma -> "3G"
        is CellLte -> "4G"
        is CellNr -> "5G"
        else -> "Unknown"
    }

    val speedPair = downloader.getBandWidthSpeed(
        networkType = type,
        hasMobileInternet = hasMobileInternet,
        currentMnc = mnc,
        activeNetworkMnc = activeNetworkMnc,
        retryCountDownload = 2,
        retryCountUpload = 2
    )

    return FTPNetworkDataEntity().apply {
        date = CommonUtils.getCurrentDate()
        this.mcc = mcc?.let { toIntSafe(it).toString() } ?: "0"
        this.mnc = mnc?.let { toIntSafe(it).toString() } ?: "0"
        technologyType = type
        band = this@prepareFTPData.band?.name ?: ""
        latitude = locationPair.first
        longitude = locationPair.second
        dlSpeed = speedPair.downloadSpeedKbps
        ulSpeed = speedPair.uploadSpeedKbps
        deviceManufacture = Build.MANUFACTURER
        deviceModel = Build.MODEL
        deviceOsVersion = Build.VERSION.SDK_INT.toString()
        internetConnectivityType = if (hasMobileInternet) "Mobile" else "Wifi"
        totalUploadVolume = speedPair.totalUploadMB
        totalDownloadVolume = speedPair.totalDownloadMB

        when (val cell = this@prepareFTPData) {
            is CellGsm -> {
                cid = toIntSafe(cell.cid) ?: 0
            }
            is CellWcdma -> {
                cid = toIntSafe(cell.cid) ?: 0
            }
            is CellLte -> {
                cid = toIntSafe(cell.cid) ?: 0
                enb = toIntSafe(cell.enb) ?: 0
                tac = toIntSafe(cell.tac) ?: 0
                rsrp = toIntSafe(cell.signal.rsrp) ?: 0
                rsrq = toIntSafe(cell.signal.rsrq) ?: 0
                snr = toIntSafe(cell.signal.snr) ?: 0
            }
            is CellNr -> {
                tac = toIntSafe(cell.tac) ?: 0
                rsrp = toIntSafe(cell.signal.ssRsrp) ?: 0
                rsrq = toIntSafe(cell.signal.ssRsrq) ?: 0
                snr = toIntSafe(cell.signal.ssSinr) ?: 0
            }
        }
    }
}

suspend fun ResponseBody?.getTotalBytes(): Int {
    val inputStream: InputStream = this?.byteStream() ?: return 0
    val byteArrayOutputStream = ByteArrayOutputStream()
    val buffer = ByteArray(2048)
    var length: Int
    return try {
        while (inputStream.read(buffer).also { length = it } != -1) {
            coroutineContext.ensureActive()
            byteArrayOutputStream.write(buffer, 0, length)
        }
        byteArrayOutputStream.size()
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        0
    } finally {
        try {
            inputStream.close()
            byteArrayOutputStream.close()
        } catch (_: Exception) {}
    }
}

fun toIntSafe(value: Any?): Int? {
    if (value == null) return null
    val str = value.toString()
    if (str.equals("null", ignoreCase = true) || str.isBlank()) return null
    return try {
        str.toDouble().toInt()
    } catch (e: Exception) {
        null
    }
}








