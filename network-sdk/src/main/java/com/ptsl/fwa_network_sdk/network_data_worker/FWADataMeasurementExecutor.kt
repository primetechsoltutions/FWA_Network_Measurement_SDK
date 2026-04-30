package com.ptsl.fwa_network_sdk.network_data_worker

import android.content.Context
import android.util.Log
import com.ptsl.fwa_network_sdk.api.ApiService
import com.ptsl.fwa_network_sdk.data_model.*
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPCellInfoGetRequest
import com.ptsl.fwa_network_sdk.data_model.entity.FTPNetworkDataEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPThresholdEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FWAAssessmentExecutionInput
import com.ptsl.fwa_network_sdk.db.NetworkDao
import com.ptsl.fwa_network_sdk.dl_ul_test.DownloadUploadHelper
import com.ptsl.fwa_network_sdk.provider.NetworkStateProvider
import com.ptsl.fwa_network_sdk.provider.ThresholdManager
import com.ptsl.fwa_network_sdk.utils.AssessmentResultMapper
import com.ptsl.fwa_network_sdk.utils.Constants
import com.ptsl.fwa_network_sdk.utils.prepareFTPData
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException


internal class FWADataMeasurementExecutor(
    private val appContext: Context,
    private val apiService: ApiService,
    private val downloader: DownloadUploadHelper,
    private val databaseDao: NetworkDao,
    private val networkStateProvider: NetworkStateProvider,
    private val thresholdManager: ThresholdManager
) {
    private val measurementLogger = MeasurementLogger(apiService, databaseDao)
    companion object {
        private const val TAG = "FWADataMeasurementExecutor"
    }

    suspend fun execute(input: FWAAssessmentExecutionInput): NetworkDataResponse {
        return try {
            val preFlightResult = PreFlightValidator.validate(networkStateProvider)
            if (preFlightResult != null) {
                measurementLogger.logValidationFailure(input, getAuth(), preFlightResult.message, preFlightResult.errorCode)
                return NetworkDataResponse(
                    status = Constants.STATUS_FAILED, testResult = Constants.RESULT_FAILED, statusCode = preFlightResult.statusCode, message = preFlightResult.message
                )
            }

            val authEntity = getAuth()
            thresholdManager.syncThresholdsIfNeeded(authEntity)

            val locationPair = LocationHelper.getCurrentLocation(appContext)
            val ftpData = getCapturedNetworkData(locationPair)

            when (ftpData.technologyType) {
                Constants.TECH_NON_4G_IGNORED -> {
                    val msg = "FWA Capture ignored: Not on Banglalink 4G network"
                    Log.w(TAG, msg)
                    measurementLogger.logValidationFailure(input, getAuth(), msg, Constants.ERR_CODE_NOT_4G)
                    return NetworkDataResponse(
                        status = Constants.STATUS_FAILED, testResult = Constants.RESULT_FAILED, statusCode = 400, message = "FWA Capture is only supported on Banglalink 4G (LTE) technology."
                    )
                }

                Constants.TECH_SKIP_MNC_MISMATCH -> {
                    val msg = "FWA Capture ignored: MNC Mismatch (Not Banglalink)"
                    Log.w(TAG, msg)
                    measurementLogger.logValidationFailure(input, getAuth(), msg, Constants.ERR_CODE_MNC_MISMATCH)
                    return NetworkDataResponse(
                        status = Constants.STATUS_FAILED, testResult = Constants.RESULT_FAILED, statusCode = 400, message = "Banglalink SIM and mobile data must be enabled for FWA Capture."
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

            val dataResult =
                AssessmentResultMapper.map(backendResponse.data?.assessmentId ?: 0, ftpData)

            NetworkDataResponse(
                status = if (isPass) Constants.STATUS_SUCCESS else Constants.STATUS_FAILED,
                testResult = if (isPass) Constants.RESULT_PASS else Constants.RESULT_FAILED,
                statusCode = if (isPass) 200 else 400,
                message = if (isPass) Constants.Measurement_Success_Message else Constants.Measurement_Failed_Message,
                data = dataResult
            )
        } catch (e: TimeoutCancellationException) {
            measurementLogger.handleException(
                input,
                getAuth(),
                e,
                Constants.ERR_CODE_TIMEOUT,
                Constants.Assessment_Timeout_Message,
                408
            )
        } catch (e: IOException) {
            measurementLogger.handleException(
                input,
                getAuth(),
                e,
                Constants.ERR_CODE_NETWORK_ERROR,
                Constants.Assessment_Failed_Message,
                400
            )
        } catch (e: Exception) {
            measurementLogger.handleException(
                input,
                getAuth(),
                e,
                Constants.ERR_CODE_EXECUTION_ERROR,
                Constants.Assessment_Error_Message,
                400
            )
        }
    }

    private fun checkThresholds(
        ftpData: FTPNetworkDataEntity, thresholds: FTPThresholdEntity
    ): Boolean {
        val isRsrpPass = Math.abs(ftpData.rsrp) <= thresholds.rsrpThreshold
        val isDlSpeedPass = ftpData.dlSpeed > thresholds.dlSpeedThreshold
        val isNbhDlThroughputPass =
            ftpData.nbhDlThroughputMbps > thresholds.nbhDlThroughputThreshold
        return isRsrpPass && isDlSpeedPass && isNbhDlThroughputPass
    }

    private suspend fun getAuth(): AuthEntity = databaseDao.getPersistentAuth() ?: AuthEntity()

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

    private suspend fun enrichDataWithCellInfo(ftpData: FTPNetworkDataEntity, auth: AuthEntity) {
        if (ftpData.cid == 0 || ftpData.enb == 0) return

        try {
            val request = FTPCellInfoGetDataRequest(
                auth = auth, data = FTPCellInfoGetRequest(eNB = ftpData.enb, cID = ftpData.cid)
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

        Log.d(
            TAG,
            "getCapturedNetworkData: activeMnc=$activeMnc, isMobileConnected=$isMobileConnected"
        )

        val activeMncClean = activeMnc.removePrefix("0")
        if (isMobileConnected && activeMncClean != Constants.BANGLALINK_MNC) {
            Log.w(TAG, "Active MNC mismatch: expected 3, got $activeMnc")
            return FTPNetworkDataEntity().apply { technologyType = Constants.TECH_SKIP_MNC_MISMATCH }
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
                val isBanglalink = cellMnc == Constants.BANGLALINK_MNC
                val isLte = cell is cz.mroczis.netmonster.core.model.cell.CellLte

                Log.d(
                    TAG,
                    "Inspecting Primary Cell: type=${cell.javaClass.simpleName}, mnc=$cellMnc, isLte=$isLte, isBL=$isBanglalink"
                )

                if (isBanglalink) {
                    if (isLte) {
                        Log.i(TAG, "✅ Found valid Banglalink 4G Primary Cell")
                        return cell.prepareFTPData(
                            locationPair, downloader, isMobileConnected, activeMnc
                        )
                    } else {
                        Log.d(
                            TAG,
                            "Found Banglalink Primary cell but it is NOT 4G (Technology: ${cell.javaClass.simpleName})"
                        )
                        foundNon4gBanglalink = true
                    }
                }
            }
        }

        return if (foundNon4gBanglalink) {
            Log.w(TAG, "❌ No Banglalink 4G cell found, only lower technologies detected")
            FTPNetworkDataEntity().apply { technologyType = Constants.TECH_NON_4G_IGNORED }
        } else {
            Log.w(TAG, "❌ No Banglalink primary connection detected in cell list")
            FTPNetworkDataEntity()
        }
    }
}


