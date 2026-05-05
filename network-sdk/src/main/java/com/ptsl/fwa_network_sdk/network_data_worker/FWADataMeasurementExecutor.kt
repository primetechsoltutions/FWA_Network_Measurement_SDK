package com.ptsl.fwa_network_sdk.network_data_worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.ptsl.fwa_network_sdk.data_model.*
import com.ptsl.fwa_network_sdk.data_model.entity.FWAAssessmentExecutionInput
import com.ptsl.fwa_network_sdk.data_model.entity.FTPNetworkDataEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPThresholdEntity
import com.ptsl.fwa_network_sdk.dl_ul_test.DownloadUploadHelper
import com.ptsl.fwa_network_sdk.provider.NetworkStateProvider
import com.ptsl.fwa_network_sdk.repository.NetworkRepository
import com.ptsl.fwa_network_sdk.repository.ThresholdRepository
import com.ptsl.fwa_network_sdk.utils.AssessmentResultMapper
import com.ptsl.fwa_network_sdk.utils.Constants
import com.ptsl.fwa_network_sdk.utils.SdkExceptionHandler
import kotlin.math.abs


import java.util.concurrent.atomic.AtomicBoolean

internal class FWADataMeasurementExecutor(
    private val appContext: Context,
    private val networkRepository: NetworkRepository,
    private val thresholdRepository: ThresholdRepository,
    private val networkStateProvider: NetworkStateProvider,
    downloader: DownloadUploadHelper,
    private val measurementLogger: MeasurementLogger
) {
    private val networkDataCapturer =
        NetworkDataCapturer(appContext, networkStateProvider, downloader)

    companion object {
        private const val TAG = "FWADataMeasurementExecutor"
    }

    suspend fun execute(input: FWAAssessmentExecutionInput): NetworkDataResponse {
        val networkCompromised = AtomicBoolean(false)
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wifiCallback = createWifiMonitorCallback(networkCompromised)

        registerWifiMonitor(cm, wifiCallback)

        return try {
            // 1. Pre-flight check
            val preFlightResult = PreFlightValidator.validate(networkStateProvider)
            if (preFlightResult != null) {
                return dispatchErrorResponse(input, preFlightResult.message, preFlightResult.errorCode)
            }

            // 2. Auth + thresholds
            val auth = networkRepository.getAuth()
            thresholdRepository.syncThresholdsIfNeeded(auth)

            // 3. Capture
            val locationPair = LocationHelper.getCurrentLocation(appContext)
            val ftpData = networkDataCapturer.capture(locationPair)

            // 4. Mid-flight validation: Check if capture was successful and on correct network
            if (ftpData.technologyType == Constants.TECH_SKIP_MNC_MISMATCH) {
                return dispatchErrorResponse(input, Constants.ERR_MSG_BANGLALINK_DATA_UNAVAILABLE, Constants.ERR_CODE_BANGLALINK_DATA_UNAVAILABLE)
            }
            if (ftpData.technologyType == Constants.TECH_NON_4G_IGNORED) {
                return dispatchErrorResponse(input, Constants.ERR_MSG_4G_REQUIRED, Constants.ERR_CODE_4G_REQUIRED)
            }

            // 5. Post-Capture Integrity Check: Re-verify state hasn't changed during the long-running speed test
            val postCaptureValidation = PreFlightValidator.validate(networkStateProvider)
            if (postCaptureValidation != null || networkCompromised.get()) {
                val message = postCaptureValidation?.message ?: Constants.ERR_MSG_WIFI_CONNECTED
                val code = postCaptureValidation?.errorCode ?: Constants.ERR_CODE_NETWORK_CHANGED
                return dispatchErrorResponse(input, message, code)
            }

            // 6. Enrich
            enrichDataWithInput(ftpData, input)
            enrichDataWithCellInfo(ftpData, auth)

            // 7. Post to backend via repository
            val backendResponse = networkRepository.postAssessmentData(auth, ftpData)

            if (networkCompromised.get()) {
                return dispatchErrorResponse(input, Constants.ERR_MSG_WIFI_CONNECTED, Constants.ERR_CODE_NETWORK_CHANGED)
            }

            // 8. Threshold evaluation
            val thresholds = thresholdRepository.getThresholds()
            val isPass = checkThresholds(ftpData, thresholds)

            // 9. Build result
            val dataResult = AssessmentResultMapper.map(
                backendResponse.data?.assessmentId ?: 0, ftpData
            )

            NetworkDataResponse(
                status = if (isPass) Constants.STATUS_SUCCESS else Constants.STATUS_FAILED,
                testResult = if (isPass) Constants.RESULT_PASS else Constants.RESULT_FAILED,
                statusCode = if (isPass) 200 else 400,
                message = if (isPass) Constants.Measurement_Success_Message
                else Constants.Measurement_Failed_Message,
                data = dataResult
            )

        } catch (e: Exception) {
            SdkExceptionHandler.handle(
                e = e,
                input = input,
                auth = networkRepository.getAuth(),
                measurementLogger = measurementLogger
            )
        } finally {
            unregisterWifiMonitor(cm, wifiCallback)
        }
    }

    // ─── Private helpers for network change observation ─────────────────────────────────────────────────────

    private fun createWifiMonitorCallback(flag: AtomicBoolean) =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                flag.set(true)
                Log.w(TAG, "Wi-Fi detected during assessment!")
            }
        }

    private fun registerWifiMonitor(cm: ConnectivityManager?, callback: ConnectivityManager.NetworkCallback) {
        try {
            val request = android.net.NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
            cm?.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register Wi-Fi monitor: ${e.message}")
        }
    }

    private fun unregisterWifiMonitor(cm: ConnectivityManager?, callback: ConnectivityManager.NetworkCallback) {
        try {
            cm?.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
            // Safe to ignore on cleanup
        }
    }

    private fun checkThresholds(
        ftpData: FTPNetworkDataEntity, thresholds: FTPThresholdEntity
    ): Boolean {
        val isRsrpPass = abs(ftpData.rsrp) <= thresholds.rsrpThreshold
        val isDlSpeedPass = ftpData.dlSpeed > thresholds.dlSpeedThreshold
        val isNbhDlThroughputPass =
            ftpData.nbhDlThroughputMbps > thresholds.nbhDlThroughputThreshold
        return isRsrpPass && isDlSpeedPass && isNbhDlThroughputPass
    }

    private fun enrichDataWithInput(
        ftpData: FTPNetworkDataEntity, input: FWAAssessmentExecutionInput
    ) {
        ftpData.apply {
            msisdn = input.msisdn
            integratedAppVersion = input.integratedAppVersion
            sdkInitiateTimeStamp = input.sdkInitiateTimeStamp
            integratedAppEventName = input.integratedAppEventName
            userLatitude = input.userLatitude
            userLongitude = input.userLongitude
        }
    }

    private suspend fun enrichDataWithCellInfo(
        ftpData: FTPNetworkDataEntity, auth: com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
    ) {
        if (ftpData.cid == 0 || ftpData.enb == 0) return

        val cellInfoList = networkRepository.fetchCellInfo(auth, ftpData.enb, ftpData.cid)
        if (cellInfoList.isNotEmpty()) {
            val cellInfo = cellInfoList.first()
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
    }

    private suspend fun dispatchErrorResponse(
        input: FWAAssessmentExecutionInput, message: String, errorCode: String
    ): NetworkDataResponse {
        val auth = networkRepository.getAuth()
        measurementLogger.logValidationFailure(input, auth, message, errorCode)

        return NetworkDataResponse(
            status = Constants.STATUS_FAILED,
            testResult = Constants.RESULT_FAILED,
            statusCode = 400,
            message = message
        )
    }
}
