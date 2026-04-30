package com.ptsl.fwa_network_sdk.network_data_worker

import android.util.Log
import com.ptsl.fwa_network_sdk.data_model.NetworkDataResponse
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FWAAssessmentExecutionInput
import com.ptsl.fwa_network_sdk.data_model.logger.EventLogModel
import com.ptsl.fwa_network_sdk.repository.LogRepository
import com.ptsl.fwa_network_sdk.utils.Constants
import com.ptsl.fwa_network_sdk.utils.NetworkEventLogger
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException

/**
 * Handles the logic for logging errors, validation failures, and exception processing.
 *
 * Delegates all persistence and remote-shipping of logs to [LogRepository],
 * keeping this class focused purely on building the correct log payload.
 */
internal class MeasurementLogger(
    private val logRepository: LogRepository
) {
    companion object {
        private const val TAG = "MeasurementLogger"
        private const val ERROR_LOG_TIMEOUT_MS = 10_000L
    }

    suspend fun handleException(
        input: FWAAssessmentExecutionInput,
        auth: AuthEntity,
        e: Exception,
        logEventName: String,
        userMessage: String,
        statusCode: Int
    ): NetworkDataResponse {
        Log.e(TAG, "$userMessage: ${e.message}")
        try {
            withTimeout(ERROR_LOG_TIMEOUT_MS) {
                logError(input, auth, e, logEventName)
            }
        } catch (_: Exception) {
            // Best-effort logging – never let a log failure propagate
        }
        return createErrorResponse(userMessage, statusCode)
    }

    suspend fun logValidationFailure(
        input: FWAAssessmentExecutionInput,
        auth: AuthEntity,
        message: String,
        errorCode: String
    ) {
        val eventName = input.integratedAppEventName

        val eventLogModel = NetworkEventLogger.createNetworkRequestFailedLog(
            auth.hostAppName, eventName, message, "Validation Logic Rejection: $errorCode", 400
        ).applyInputFields(input)

        logRepository.flushLogs(auth, eventLogModel)
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private suspend fun logError(
        input: FWAAssessmentExecutionInput,
        auth: AuthEntity,
        e: Exception,
        eventName: String
    ) {
        var statusCode = 0
        val errorMessage = when (e) {
            is HttpException -> {
                statusCode = e.code()
                val errorBody = try {
                    e.response()?.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }
                "HTTP error: ${e.message}${if (errorBody != null) " | Body: $errorBody" else ""}"
            }
            is IOException -> "Network error: ${e.message}"
            else           -> "Unexpected error: ${e.message}"
        }

        val eventLogModel = NetworkEventLogger.createNetworkRequestFailedLog(
            auth.hostAppName, eventName, errorMessage, e.stackTraceToString(), statusCode
        ).applyInputFields(input)

        logRepository.flushLogs(auth, eventLogModel)
    }

    private fun createErrorResponse(message: String, statusCode: Int): NetworkDataResponse {
        return NetworkDataResponse(
            status = Constants.STATUS_FAILED,
            testResult = Constants.RESULT_FAILED,
            statusCode = statusCode,
            message = message
        )
    }

    /** Copies [FWAAssessmentExecutionInput] fields onto an [EventLogModel] in one place. */
    private fun EventLogModel.applyInputFields(input: FWAAssessmentExecutionInput): EventLogModel {
        return this.apply {
            msisdn = input.msisdn
            integratedAppVersion = input.integratedAppVersion
            sdkInitiateTimeStamp = input.sdkInitiateTimeStamp
            integratedAppEventName = input.integratedAppEventName
            userLatitude = input.userLatitude
            userLongitude = input.userLongitude
        }
    }
}
