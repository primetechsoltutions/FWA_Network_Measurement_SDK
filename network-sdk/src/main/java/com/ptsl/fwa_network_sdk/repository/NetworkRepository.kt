package com.ptsl.fwa_network_sdk.repository

import android.util.Log
import com.ptsl.fwa_network_sdk.api.ApiService
import com.ptsl.fwa_network_sdk.data_model.BaseResponse
import com.ptsl.fwa_network_sdk.data_model.FTPCellInfoGetDataRequest
import com.ptsl.fwa_network_sdk.data_model.FTPNetworkDataRequest
import com.ptsl.fwa_network_sdk.data_model.entity.AssessmentDataResponseEntity
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FTPCellInfoGetRequest
import com.ptsl.fwa_network_sdk.data_model.entity.FTPCellInfoGetResponse
import com.ptsl.fwa_network_sdk.data_model.entity.FTPNetworkDataEntity
import com.ptsl.fwa_network_sdk.db.NetworkDao

/**
 * Repository interface for network assessment operations.
 *
 * Acts as the single source of truth (SSOT) for all data operations that
 * involve either the remote API or the local Room database. Callers should
 * never talk directly to [ApiService] or [NetworkDao] for assessment data.
 */
internal interface NetworkRepository {

    // ─── Auth ────────────────────────────────────────────────────────────────

    /** Persist [auth] in the local database, replacing any existing record. */
    suspend fun saveAuth(auth: AuthEntity)

    /** Return the locally-persisted [AuthEntity], or a default empty one if none exists. */
    suspend fun getAuth(): AuthEntity

    // ─── Assessment ──────────────────────────────────────────────────────────

    /**
     * Post the captured [ftpData] to the backend.
     * @return The raw [BaseResponse] from the server.
     */
    suspend fun postAssessmentData(
        auth: AuthEntity,
        ftpData: FTPNetworkDataEntity
    ): BaseResponse<AssessmentDataResponseEntity>

    // ─── Cell Info ───────────────────────────────────────────────────────────

    /**
     * Fetch cell-info from the backend for the given eNB / CID pair.
     * Returns an empty list on failure so callers can continue without enrichment.
     */
    suspend fun fetchCellInfo(
        auth: AuthEntity,
        enb: Int,
        cid: Int
    ): List<FTPCellInfoGetResponse>
}

// ─────────────────────────────────────────────────────────────────────────────

internal class NetworkRepositoryImpl(
    private val apiService: ApiService,
    private val dao: NetworkDao
) : NetworkRepository {

    companion object {
        private const val TAG = "NetworkRepository"
    }

    // ─── Auth ────────────────────────────────────────────────────────────────

    override suspend fun saveAuth(auth: AuthEntity) {
        try {
            dao.insertAuthData(auth)
            Log.d(TAG, "Auth saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save auth: ${e.message}")
        }
    }

    override suspend fun getAuth(): AuthEntity {
        return try {
            dao.getPersistentAuth() ?: AuthEntity()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve auth, using default: ${e.message}")
            AuthEntity()
        }
    }

    // ─── Assessment ──────────────────────────────────────────────────────────

    override suspend fun postAssessmentData(
        auth: AuthEntity,
        ftpData: FTPNetworkDataEntity
    ): BaseResponse<AssessmentDataResponseEntity> {
        Log.d(TAG, "Posting assessment data to backend")
        return apiService.postFTPNetworkData(FTPNetworkDataRequest(auth = auth, data = ftpData))
    }

    // ─── Cell Info ───────────────────────────────────────────────────────────

    override suspend fun fetchCellInfo(
        auth: AuthEntity,
        enb: Int,
        cid: Int
    ): List<FTPCellInfoGetResponse> {
        return try {
            val request = FTPCellInfoGetDataRequest(
                auth = auth,
                data = FTPCellInfoGetRequest(eNB = enb, cID = cid)
            )
            val response = apiService.postFTPCellInfo(request)
            if (response.statusCode == 200 && !response.data.isNullOrEmpty()) {
                Log.d(TAG, "Cell info fetched: ${response.data!!.size} record(s)")
                response.data!!
            } else {
                Log.w(TAG, "Cell info fetch returned no data (statusCode=${response.statusCode})")
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cell info fetch failed: ${e.message}")
            emptyList()
        }
    }
}
