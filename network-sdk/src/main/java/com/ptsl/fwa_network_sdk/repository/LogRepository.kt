package com.ptsl.fwa_network_sdk.repository

import android.util.Log
import com.ptsl.fwa_network_sdk.api.ApiService
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.logger.EventLogModel
import com.ptsl.fwa_network_sdk.data_model.logger.LogDataWrapper
import com.ptsl.fwa_network_sdk.db.NetworkDao

/**
 * Repository interface for SDK event-log operations.
 *
 * Encapsulates the "collect-locally, flush-remotely" log pattern so that
 * [MeasurementLogger] (and any future consumer) works purely through this
 * abstraction rather than touching [NetworkDao] / [ApiService] directly.
 */
internal interface LogRepository {

    /** Persist a single [log] entry in the local database. */
    suspend fun saveLog(log: EventLogModel)

    /** Return all locally-cached log entries. */
    suspend fun getCachedLogs(): List<EventLogModel>

    /** Return the count of locally-cached log entries. */
    suspend fun getCachedLogCount(): Long

    /** Delete all locally-cached log entries. */
    suspend fun clearCachedLogs()

    /**
     * Attempt to flush all pending logs (cached + [newLog]) to the remote API.
     *
     * Strategy:
     *  1. Load cached logs from DB.
     *  2. Append [newLog] to the batch.
     *  3. POST the batch to the server.
     *  4. On success → delete cached logs from DB (they have been shipped).
     *  5. On failure → persist [newLog] so it is retried on the next flush.
     *
     * @param auth  Authentication context for the API call.
     * @param newLog The freshly-created log entry to include in the flush.
     */
    suspend fun flushLogs(auth: AuthEntity, newLog: EventLogModel)
}

// ─────────────────────────────────────────────────────────────────────────────

internal class LogRepositoryImpl(
    private val apiService: ApiService,
    private val dao: NetworkDao
) : LogRepository {

    companion object {
        private const val TAG = "LogRepository"
    }

    override suspend fun saveLog(log: EventLogModel) {
        try {
            dao.insertNetworkDataLogIntoDB(log)
            Log.d(TAG, "Log persisted locally: ${log.integratedAppEventName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist log: ${e.message}")
        }
    }

    override suspend fun getCachedLogs(): List<EventLogModel> {
        return try {
            dao.getNetworkDataLogEvent()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve cached logs: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getCachedLogCount(): Long {
        return try {
            dao.getNetworkDataLogEventCount()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to count cached logs: ${e.message}")
            0L
        }
    }

    override suspend fun clearCachedLogs() {
        try {
            dao.deleteNetworkDataLogEvent()
            Log.d(TAG, "Cleared all cached logs from DB")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cached logs: ${e.message}")
        }
    }

    override suspend fun flushLogs(auth: AuthEntity, newLog: EventLogModel) {
        try {
            val logsToSend = mutableListOf<EventLogModel>()
            val cachedCount = getCachedLogCount()

            if (cachedCount > 0) {
                logsToSend.addAll(getCachedLogs())
            }
            logsToSend.add(newLog)

            apiService.postNetworkDataLogs(LogDataWrapper(auth, ArrayList(logsToSend)))
            Log.i(TAG, "Flushed ${logsToSend.size} log(s) to server")

            // Only clear cached logs after a successful API call
            if (cachedCount > 0) {
                clearCachedLogs()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Log flush failed, persisting locally for retry: ${e.message}")
            saveLog(newLog)
        }
    }
}
