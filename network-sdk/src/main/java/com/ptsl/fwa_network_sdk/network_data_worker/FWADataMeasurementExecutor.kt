package com.ptsl.fwa_network_sdk.network_data_worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.ptsl.fwa_network_sdk.api.ApiService
import com.ptsl.fwa_network_sdk.data_model.*
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPCellInfoGetRequest
import com.ptsl.fwa_network_sdk.data_model.entity.FTPNetworkDataEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPThresholdEntity
import com.ptsl.fwa_network_sdk.data_model.logger.EventLogModel
import com.ptsl.fwa_network_sdk.data_model.logger.LogDataWrapper
import com.ptsl.fwa_network_sdk.db.NetworkDao
import com.ptsl.fwa_network_sdk.dl_ul_test.DownloadUploadHelper
import com.ptsl.fwa_network_sdk.provider.NetworkStateProvider
import com.ptsl.fwa_network_sdk.provider.ThresholdManager
import com.ptsl.fwa_network_sdk.utils.NetworkEventLogger
import com.ptsl.fwa_network_sdk.utils.prepareFTPData
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException

data class FWAAssessmentExecutionInput(
    val msisdn: String,
    val integratedAppVersion: String,
    val sdkInitiateTimeStamp: String,
    val integratedAppEventName: String,
    val userLatitude: Double,
    val userLongitude: Double
)

internal class FWADataMeasurementExecutor(
    private val appContext: Context,
    private val apiService: ApiService,
    private val downloader: DownloadUploadHelper,
    private val databaseDao: NetworkDao,
    private val networkStateProvider: NetworkStateProvider,
    private val thresholdManager: ThresholdManager
) {
    companion object {
        private const val TAG = "FWADataMeasurementExecutor"
        private const val ERROR_LOG_TIMEOUT_MS = 10_000L
    }

    suspend fun execute(input: FWAAssessmentExecutionInput): NetworkDataResponse {
        return try {
            val preFlightCheck = performPreFlightChecks()
            if (preFlightCheck != null) {
                logValidationFailure(input, preFlightCheck.message, preFlightCheck.errorCode)
                return createErrorResponse(preFlightCheck.message, preFlightCheck.statusCode)
            }

            val authEntity = getAuth()
            thresholdManager.syncThresholdsIfNeeded(authEntity)

            val locationPair = LocationHelper.getCurrentLocation(appContext)
            val ftpData = getCapturedNetworkData(locationPair)

            when (ftpData.technologyType) {
                "NON_4G_IGNORED" -> {
                    val msg = "FWA Capture ignored: Not on Banglalink 4G network"
                    Log.w(TAG, msg)
                    logValidationFailure(input, msg, "FTP_CAPTURE_NOT_4G")
                    return createErrorResponse(
                        "FWA Capture is only supported on Banglalink 4G (LTE) technology.",
                        400
                    )
                }
                "SKIP_MNC_MISMATCH" -> {
                    val msg = "FWA Capture ignored: MNC Mismatch (Not Banglalink)"
                    Log.w(TAG, msg)
                    logValidationFailure(input, msg, "FTP_CAPTURE_MNC_MISMATCH")
                    return createErrorResponse(
                        "Banglalink SIM and mobile data must be enabled for FWA Capture.",
                        400
                    )
                }
            }

            enrichDataWithInput(ftpData, input)
            enrichDataWithCellInfo(ftpData, authEntity)

            val backendResponse = apiService.postFTPNetworkData(
                FTPNetworkDataRequest(auth = authEntity, data = ftpData)
            )

            val thresholds = thresholdManager.getThresholds()
            val isPass = checkThresholds(ftpData, thresholds)

            val dataResult = createAssessmentResult(backendResponse.data?.assessmentId ?: 0, ftpData)

            NetworkDataResponse(
                status = if (isPass) "Success" else "Failed",
                testResult = if (isPass) "Pass" else "Failed",
                statusCode = if (isPass) 200 else 400,
                message = if (isPass) "Your network assessment was successful." else "Your network assessment failed.",
                data = dataResult
            )
        } catch (e: TimeoutCancellationException) {
            handleException(input, e, "FTP_CAPTURE_TIMEOUT", "Network assessment timed out. Please check your internet connection.", 408)
        } catch (e: IOException) {
            handleException(input, e, "FTP_CAPTURE_NETWORK_ERROR", "Network assessment failed. Please check your internet connection.", 400)
        } catch (e: Exception) {
            handleException(input, e, "FTP_CAPTURE_EXECUTION_ERROR", "Network assessment failed due to internal error.", 400)
        }
    }

    private fun checkThresholds(ftpData: FTPNetworkDataEntity, thresholds: FTPThresholdEntity): Boolean {
        val isRsrpPass = Math.abs(ftpData.rsrp) <= thresholds.rsrpThreshold
        val isDlSpeedPass = ftpData.dlSpeed > thresholds.dlSpeedThreshold
        val isNbhDlThroughputPass = ftpData.nbhDlThroughputMbps > thresholds.nbhDlThroughputThreshold
        return isRsrpPass && isDlSpeedPass && isNbhDlThroughputPass
    }

    private fun createAssessmentResult(assessmentId: Long, ftpData: FTPNetworkDataEntity): AssessmentResult {
        return AssessmentResult(
            assessmentId = assessmentId,
            networkData = NetworkMetrics(
                RSRP = ftpData.rsrp,
                SNR = ftpData.snr,
                RSRQ = ftpData.rsrq
            ),
            cellInfo = CellMetadata(
                cellName = ftpData.cellName,
                eNodeBName = ftpData.eNodeBName,
                nbhDlThroughputMbps = ftpData.nbhDlThroughputMbps,
                nbhTrafficGB = ftpData.nbhTrafficGb
            ),
            speedPair = SpeedMetrics(
                ulSpeedKbps = ftpData.ulSpeed,
                dlSpeedKbps = ftpData.dlSpeed
            ),
            userInfo = UserMetadata(
                deviceManufacture = ftpData.deviceManufacture,
                deviceModel = ftpData.deviceModel,
                deviceOsVersion = ftpData.deviceOsVersion,
                latitude = ftpData.latitude,
                longitude = ftpData.longitude,
                msisdn = ftpData.msisdn,
            )
        )
    }

    private suspend fun handleException(
        input: FWAAssessmentExecutionInput,
        e: Exception,
        logEventName: String,
        userMessage: String,
        statusCode: Int
    ): NetworkDataResponse {
        Log.e(TAG, "$userMessage: ${e.message}")
        try {
            withTimeout(ERROR_LOG_TIMEOUT_MS) {
                logError(input, e, logEventName)
            }
        } catch (_: Exception) {
            // Best effort logging
        }
        return createErrorResponse(userMessage, statusCode)
    }

    private fun createErrorResponse(message: String, statusCode: Int): NetworkDataResponse {
        return NetworkDataResponse(
            status = "Failed",
            testResult = "Failed",
            statusCode = statusCode,
            message = message
        )
    }

    private data class PreFlightResult(val message: String, val errorCode: String, val statusCode: Int)

    private fun performPreFlightChecks(): PreFlightResult? {
        return when {
            !networkStateProvider.hasLocationPermissions() ->
                PreFlightResult("Permission are required for FWA Capture", "FTP_PERMISSION_DENIED", 400)

            !networkStateProvider.isGpsEnabled() ->
                PreFlightResult("GPS disable please enable GPS", "FTP_GPS_DISABLED", 400)

            !networkStateProvider.isInternetAvailable() ->
                PreFlightResult("Internet connectivity is mandatory for network assessment.", "FTP_INTERNET_UNAVAILABLE", 400)

            networkStateProvider.isWifiConnected() ->
                PreFlightResult("FWA Capture requires mobile data. Please disable Wi-Fi and ensure Banglalink 4G is active.", "FTP_WIFI_CONNECTED", 400)

            !networkStateProvider.isMobileNetworkConnected() ->
                PreFlightResult("Mobile data connection is required for FWA Capture. Please enable mobile data.", "FTP_MOBILE_DATA_REQUIRED", 400)

            !networkStateProvider.is4GConnected() ->
                PreFlightResult("4G/LTE connection is required for FWA Capture. Currently not on 4G.", "FTP_4G_REQUIRED", 400)

            !networkStateProvider.isBanglalinkDataEnabled() ->
                PreFlightResult("Banglalink SIM and mobile data must be enabled for FWA Capture.", "FTP_BANGLALINK_DATA_UNAVAILABLE", 400)

            else -> null
        }
    }

    private suspend fun getAuth(): AuthEntity = databaseDao.getPersistentAuth() ?: AuthEntity()

    private fun enrichDataWithInput(ftpData: FTPNetworkDataEntity, input: FWAAssessmentExecutionInput) {
        ftpData.apply {
            msisdn = input.msisdn
            integratedAppVersion = input.integratedAppVersion
            sdkInitiateTimeStamp = input.sdkInitiateTimeStamp
            integratedAppEventName = input.integratedAppEventName
            userLatitude = input.userLatitude
            userLongitude = input.userLongitude
        }
    }

    private suspend fun enrichDataWithCellInfo(ftpData: FTPNetworkDataEntity, auth: AuthEntity) {
        if (ftpData.cid == 0 || ftpData.enb == 0) return

        try {
            val request = FTPCellInfoGetDataRequest(
                auth = auth,
                data = FTPCellInfoGetRequest(eNB = ftpData.enb, cID = ftpData.cid)
            )
            val response = apiService.postFTPCellInfo(request)
            if (response.statusCode == 200 && !response.data.isNullOrEmpty()) {
                val cellInfo = response.data!!.first()
                ftpData.apply {
                    eNodeBName = cellInfo.eNodeBName
                    cellName = cellInfo.cellName
                    eNodeBId = cellInfo.eNodeBId
                    sector = cellInfo.sector
                    nbhDlThroughputMbps = cellInfo.nbhDlThroughputMbps
                    nbhTrafficGb = cellInfo.nbhTrafficGb
                }
                Log.d(TAG, "Cell Info Enriched: ${cellInfo.cellName}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cell Enrichment failed: ${e.message}")
        }
    }

    private suspend fun getCapturedNetworkData(locationPair: Pair<Double, Double>): FTPNetworkDataEntity {
        val isMobileConnected = networkStateProvider.isMobileNetworkConnected()
        val activeMnc = if (isMobileConnected) networkStateProvider.getActiveNetworkMNC() else "-1"

        Log.d(TAG, "getCapturedNetworkData: activeMnc=$activeMnc, isMobileConnected=$isMobileConnected")

        val activeMncClean = activeMnc.removePrefix("0")
        if (isMobileConnected && activeMncClean != "3") {
            Log.w(TAG, "Active MNC mismatch: expected 3, got $activeMnc")
            return FTPNetworkDataEntity().apply { technologyType = "SKIP_MNC_MISMATCH" }
        }

        val cells = try {
            if (networkStateProvider.hasLocationPermissions()) NetMonsterFactory.get(appContext).getCells()
            else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cells: ${e.message}")
            null
        }

        if (cells.isNullOrEmpty()) {
            Log.w(TAG, "No cells detected by NetMonster")
            return FTPNetworkDataEntity()
        }

        var foundNon4gBanglalink = false

        for (cell in cells) {
            if (cell.connectionStatus is PrimaryConnection) {
                val cellMnc = cell.network?.mnc?.toString()?.removePrefix("0") ?: ""
                val isBanglalink = cellMnc == "3"
                val isLte = cell is cz.mroczis.netmonster.core.model.cell.CellLte

                Log.d(TAG, "Inspecting Primary Cell: type=${cell.javaClass.simpleName}, mnc=$cellMnc, isLte=$isLte, isBL=$isBanglalink")

                if (isBanglalink) {
                    if (isLte) {
                        Log.i(TAG, "✅ Found valid Banglalink 4G Primary Cell")
                        return cell.prepareFTPData(locationPair, downloader, isMobileConnected, activeMnc)
                    } else {
                        Log.d(TAG, "Found Banglalink Primary cell but it is NOT 4G (Technology: ${cell.javaClass.simpleName})")
                        foundNon4gBanglalink = true
                    }
                }
            }
        }

        return if (foundNon4gBanglalink) {
            Log.w(TAG, "❌ No Banglalink 4G cell found, only lower technologies detected")
            FTPNetworkDataEntity().apply { technologyType = "NON_4G_IGNORED" }
        } else {
            Log.w(TAG, "❌ No Banglalink primary connection detected in cell list")
            FTPNetworkDataEntity()
        }
    }

    private suspend fun logValidationFailure(
        input: FWAAssessmentExecutionInput,
        message: String,
        errorCode: String
    ) {
        val auth = getAuth()
        val eventName = input.integratedAppEventName

        val eventLogModel = NetworkEventLogger.createNetworkRequestFailedLog(
            auth.hostAppName, eventName, message, "Validation Logic Rejection: $errorCode", 400
        ).apply {
            msisdn = input.msisdn
            integratedAppVersion = input.integratedAppVersion
            sdkInitiateTimeStamp = input.sdkInitiateTimeStamp
            integratedAppEventName = eventName
            userLatitude = input.userLatitude
            userLongitude = input.userLongitude
        }

        preparedLogEventData(auth, eventLogModel)
    }

    private suspend fun logError(
        input: FWAAssessmentExecutionInput,
        e: Exception,
        eventName: String
    ) {
        val auth = getAuth()
        var statusCode = 0
        val errorMessage = when (e) {
            is HttpException -> {
                statusCode = e.code()
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                "HTTP error: ${e.message}${if (errorBody != null) " | Body: $errorBody" else ""}"
            }
            is IOException -> "Network error: ${e.message}"
            else -> "Unexpected error: ${e.message}"
        }

        val eventLogModel = NetworkEventLogger.createNetworkRequestFailedLog(
            auth.hostAppName, eventName, errorMessage, e.stackTraceToString(), statusCode
        ).apply {
            msisdn = input.msisdn
            integratedAppVersion = input.integratedAppVersion
            sdkInitiateTimeStamp = input.sdkInitiateTimeStamp
            integratedAppEventName = eventName
            userLatitude = input.userLatitude
            userLongitude = input.userLongitude
        }

        preparedLogEventData(auth, eventLogModel)
    }

    private suspend fun preparedLogEventData(auth: AuthEntity, eventLogModel: EventLogModel) {
        try {
            val logsToSend = mutableListOf<EventLogModel>()
            val cachedCount = databaseDao.getNetworkDataLogEventCount()
            if (cachedCount > 0) {
                logsToSend.addAll(databaseDao.getNetworkDataLogEvent())
            }
            logsToSend.add(eventLogModel)

            apiService.postNetworkDataLogs(LogDataWrapper(auth, ArrayList(logsToSend)))

            if (cachedCount > 0) {
                databaseDao.deleteNetworkDataLogEvent()
            }
        } catch (e: Exception) {
            databaseDao.insertNetworkDataLogIntoDB(eventLogModel)
        }
    }
}


