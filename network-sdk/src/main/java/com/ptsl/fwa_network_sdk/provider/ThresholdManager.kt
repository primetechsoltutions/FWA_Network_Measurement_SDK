package com.ptsl.fwa_network_sdk.provider

import android.util.Log
import com.ptsl.fwa_network_sdk.api.ApiService
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPThresholdEntity
import com.ptsl.fwa_network_sdk.db.NetworkDao
import com.ptsl.fwa_network_sdk.utils.CommonUtils

interface ThresholdManager {
    suspend fun getThresholds(): FTPThresholdEntity
    suspend fun syncThresholdsIfNeeded(auth: AuthEntity)
}

internal class ThresholdManagerImpl(
    private val apiService: ApiService,
    private val databaseDao: NetworkDao
) : ThresholdManager {

    companion object {
        private const val TAG = "ThresholdManager"
    }

    override suspend fun getThresholds(): FTPThresholdEntity {
        return databaseDao.getFTPThresholds() ?: FTPThresholdEntity()
    }

    override suspend fun syncThresholdsIfNeeded(auth: AuthEntity) {
        try {
            val cached = databaseDao.getFTPThresholds()
            val shouldFetch = cached == null || (CommonUtils.getCurrentDate() != cached.lastUpdated)

            if (shouldFetch) {
                val response = apiService.getFTPThresholds(auth)
                if (response.statusCode == 200 && response.data != null) {
                    val newThresholds = response.data?.apply {
                        lastUpdated = CommonUtils.getCurrentDate()
                        id = 1
                    }
                    databaseDao.insertFTPThresholds(newThresholds ?: FTPThresholdEntity())
                    Log.i(TAG, "Thresholds sync successful")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Threshold Update failed: ${e.message}")
        }
    }
}
