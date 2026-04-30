package com.ptsl.fwa_network_sdk.dl_ul_test

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ptsl.fwa_network_sdk.api.ApiService
import com.ptsl.fwa_network_sdk.data_model.BandWidth
import com.ptsl.fwa_network_sdk.data_model.BandwidthTestResult
import com.ptsl.fwa_network_sdk.data_model.BaseResponse
import com.ptsl.fwa_network_sdk.utils.*
import kotlinx.coroutines.ensureActive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

class DownloadUploadHelper(private val apiService: ApiService) {

    private val gson = Gson()

    suspend fun getBandWidthSpeed(
        networkType: String = "2G",
        retryCountDownload: Int = 1,
        retryCountUpload: Int = 1,
        hasMobileInternet: Boolean = false,
        currentMnc: String?,
        activeNetworkMnc: String = "-1"
    ): BandwidthTestResult {

        if (!hasMobileInternet || currentMnc?.toIntOrNull() != activeNetworkMnc.toIntOrNull()) {
            return BandwidthTestResult(0.0, 0.0, 0, 0)
        }

        val stopwatch = Stopwatch()

        // ---------------- DOWNLOAD ----------------
        var uploadRequestBodyModel: BaseResponse<BandWidth>? = null
        var totalDownloadBytes = 0L
        val downloadSpeeds = mutableListOf<Double>()

        repeat(retryCountDownload) {
            try {
                stopwatch.start()
                val response = apiService.getBandwidthFile(networkType)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        stopwatch.stop()
                        val timeSec = stopwatch.elapsedSeconds()
                        
                        val bytes: Int
                        if (uploadRequestBodyModel == null) {
                            val bodyString = readBodyStringCancellably(body)
                            uploadRequestBodyModel = gson.fromJson(
                                bodyString,
                                object : TypeToken<BaseResponse<BandWidth>>() {}.type
                            )
                            bytes = bodyString.length
                        } else {
                            bytes = body.getTotalBytes()
                        }

                        totalDownloadBytes += bytes
                        if (timeSec > 0) {
                            val speedKbps = (bytes.toDouble() * 8 / 1000) / timeSec
                            downloadSpeeds.add(speedKbps)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Clean Code: Log exception to prevent silent failures during speed tests
                android.util.Log.w("DownloadUploadHelper", "Download test iteration failed: ${e.message}")
            } finally {
                coroutineContext.ensureActive()
                stopwatch.reset()
            }
        }

        // ---------------- UPLOAD ----------------
        var totalUploadBytes = 0L
        val uploadSpeeds = mutableListOf<Double>()

        repeat(retryCountUpload) {
            try {
                uploadRequestBodyModel?.let { reqModel ->
                    stopwatch.start()
                    val body = RequestBody.create(
                        "application/json".toMediaTypeOrNull(),
                        gson.toJson(reqModel)
                    )

                    val response = apiService.saveBandwidthFile(body)
                    if (response.isSuccessful) {
                        stopwatch.stop()
                        val timeSec = stopwatch.elapsedSeconds()
                        val bytes = body.contentLength().toInt()

                        totalUploadBytes += bytes
                        if (timeSec > 0) {
                            val speedKbps = (bytes.toDouble() * 8 / 1000) / timeSec
                            uploadSpeeds.add(speedKbps)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Clean Code: Log exception to prevent silent failures during speed tests
                android.util.Log.w("DownloadUploadHelper", "Upload test iteration failed: ${e.message}")
            } finally {
                coroutineContext.ensureActive()
                stopwatch.reset()
            }
        }

        coroutineContext.ensureActive()

        val avgDownloadSpeed = if (downloadSpeeds.isNotEmpty()) downloadSpeeds.average() else 0.0
        val avgUploadSpeed = if (uploadSpeeds.isNotEmpty()) uploadSpeeds.average() else 0.0

        return BandwidthTestResult(
            downloadSpeedKbps = (avgDownloadSpeed * 100).roundToInt() / 100.0,
            uploadSpeedKbps = (avgUploadSpeed * 100).roundToInt() / 100.0,
            totalDownloadBytes = totalDownloadBytes,
            totalUploadBytes = totalUploadBytes
        )
    }

    private suspend fun readBodyStringCancellably(body: ResponseBody): String {
        val inputStream = body.byteStream()
        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        var length: Int
        try {
            while (inputStream.read(buffer).also { length = it } != -1) {
                coroutineContext.ensureActive()
                bos.write(buffer, 0, length)
            }
            return bos.toString(StandardCharsets.UTF_8.name())
        } finally {
            inputStream.close()
            bos.close()
        }
    }
}