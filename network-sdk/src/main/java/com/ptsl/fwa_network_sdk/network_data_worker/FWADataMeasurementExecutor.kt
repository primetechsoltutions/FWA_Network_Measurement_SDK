package com.ptsl.fwa_network_sdk.network_data_worker

import android.content.Context
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

            // 4. Enrich
            enrichDataWithInput(ftpData, input)
            enrichDataWithCellInfo(ftpData, auth)

            // 5. Post to backend via repository
            val backendResponse = networkRepository.postAssessmentData(auth, ftpData)
            // 6. Threshold evaluation
            val thresholds = thresholdRepository.getThresholds()
            val isPass = checkThresholds(ftpData, thresholds)

            // 7. Build result
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
