package com.ptsl.fwa_network_sdk.repository

import android.util.Log
import com.ptsl.fwa_network_sdk.api.ApiService
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPThresholdEntity
import com.ptsl.fwa_network_sdk.db.NetworkDao
import com.ptsl.fwa_network_sdk.utils.CommonUtils

/**
 * Repository interface for FTP threshold data.
 *
 * Replaces the [com.ptsl.fwa_network_sdk.provider.ThresholdManager] /
 * [com.ptsl.fwa_network_sdk.provider.ThresholdManagerImpl] pair so that the
 * threshold access follows the same Repository pattern as the rest of the SDK.
 *
 * Cache strategy: thresholds are fetched from the backend **once per calendar
 * day**. All other reads are served from the local Room cache.
 */
internal interface ThresholdRepository {

    /**
     * Return the cached [FTPThresholdEntity].
     * Falls back to a default (zeroed) entity if the cache is empty.
     */
    suspend fun getThresholds(): FTPThresholdEntity

    /**
     * Synchronise thresholds with the backend if the local cache is stale
     * (i.e. absent, or last updated on a different calendar day).
     *
     * The result is persisted to the local DB so subsequent calls within the
     * same day are served from cache without a network round-trip.
     *
     * @param auth Authentication context required by the API.
     */
    suspend fun syncThresholdsIfNeeded(auth: AuthEntity)
}

// ─────────────────────────────────────────────────────────────────────────────

internal class ThresholdRepositoryImpl(
    private val apiService: ApiService,
    private val dao: NetworkDao
) : ThresholdRepository {

    companion object {
        private const val TAG = "ThresholdRepository"
    }

    override suspend fun getThresholds(): FTPThresholdEntity {
        return try {
            dao.getFTPThresholds() ?: FTPThresholdEntity()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read thresholds from DB, using defaults: ${e.message}")
            FTPThresholdEntity()
        }
    }

    override suspend fun syncThresholdsIfNeeded(auth: AuthEntity) {
        try {
            val cached = dao.getFTPThresholds()
            val today = CommonUtils.getCurrentDate()
            val isStale = cached == null || cached.lastUpdated != today

            if (!isStale) {
                Log.d(TAG, "Thresholds are up-to-date (last updated: ${cached.lastUpdated})")
                return
            }

            Log.d(TAG, "Thresholds are stale – fetching from backend")
            val response = apiService.getFTPThresholds(auth)

            if (response.statusCode == 200 && response.data != null) {
                val fresh = response.data!!.apply {
                    lastUpdated = today
                    id = 1   // Single-row cache; keep a stable PK
                }
                dao.insertFTPThresholds(fresh)
                Log.i(TAG, "Thresholds synced successfully")
            } else {
                Log.w(TAG, "Backend returned unexpected response (statusCode=${response.statusCode})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Threshold sync failed: ${e.message}")
            // Non-fatal: the executor will fall back to whatever is cached (or defaults)
        }
    }
}
