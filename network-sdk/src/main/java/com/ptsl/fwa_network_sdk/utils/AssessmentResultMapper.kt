package com.ptsl.fwa_network_sdk.utils

import com.ptsl.fwa_network_sdk.data_model.AssessmentResult
import com.ptsl.fwa_network_sdk.data_model.CellMetadata
import com.ptsl.fwa_network_sdk.data_model.NetworkMetrics
import com.ptsl.fwa_network_sdk.data_model.SpeedMetrics
import com.ptsl.fwa_network_sdk.data_model.UserMetadata
import com.ptsl.fwa_network_sdk.data_model.entity.FTPNetworkDataEntity

/**
 * Mapper class responsible for converting FTPNetworkDataEntity to an AssessmentResult.
 * This separates the data mapping concern from the execution logic.
 */
object AssessmentResultMapper {

    fun map(assessmentId: Long, ftpData: FTPNetworkDataEntity): AssessmentResult {
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
}
