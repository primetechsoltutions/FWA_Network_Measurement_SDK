package com.ptsl.fwa_network_sdk.utils

import android.util.Log
import com.ptsl.fwa_network_sdk.data_model.NetworkDataResponse
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FWAAssessmentExecutionInput
import com.ptsl.fwa_network_sdk.network_data_worker.MeasurementLogger
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException

/**
 * Exception handler for the FWA SDK measurement pipeline.
 *
 * ### Responsibilities
 * 1. **Classify** any thrown [Exception] into a typed [SdkErrorContext]
 *    (error code, user message, HTTP status code) via [resolve].
 * 2. **Handle** the exception end-to-end via [handle]:
 *    classify → log via [MeasurementLogger] → return a failed [NetworkDataResponse].
 *
 * Callers (e.g. [com.ptsl.fwa_network_sdk.network_data_worker.FWADataMeasurementExecutor])
 * need only a **single** catch block:
 * ```kotlin
 * } catch (e: Exception) {
 *     SdkExceptionHandler.handle(e, input, auth, measurementLogger)
 * }
 * ```
 *
 * ### Exception priority order
 * The `when` expression intentionally checks [TimeoutCancellationException] **before**
 * [IOException] and the general [Exception] branch, because
 * [TimeoutCancellationException] is a subtype of [kotlinx.coroutines.CancellationException]
 * (which is itself an [Exception]) and would otherwise be swallowed by the catch-all.
 */
internal object SdkExceptionHandler {

    private const val TAG = "SdkExceptionHandler"

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Classify [e] into an [SdkErrorContext] without performing any side-effects.
     *
     * Use this when you only need the classification (e.g. unit tests or custom
     * logging paths) without triggering [MeasurementLogger].
     */
    fun resolve(e: Exception): SdkErrorContext = when (e) {
        is TimeoutCancellationException -> SdkErrorContext(
            errorCode   = Constants.ERR_CODE_TIMEOUT,
            userMessage = Constants.Assessment_Timeout_Message,
            statusCode  = 408
        )
        is IOException -> SdkErrorContext(
            errorCode   = Constants.ERR_CODE_NETWORK_ERROR,
            userMessage = Constants.Assessment_Failed_Message,
            statusCode  = 400
        )
        else -> SdkErrorContext(
            errorCode   = Constants.ERR_CODE_EXECUTION_ERROR,
            userMessage = Constants.Assessment_Error_Message,
            statusCode  = 400
        )
    }

    /**
     * Fully handle [e]:
     *  1. Resolve to [SdkErrorContext].
     *  2. Log the exception via [measurementLogger] (best-effort; never throws).
     *  3. Return a [NetworkDataResponse] representing the failure.
     *
     * @param e                 The caught exception.
     * @param input             Execution context used to build the log payload.
     * @param auth              Authentication context used to build the log payload.
     * @param measurementLogger Logger that persists / ships the error event.
     */
    suspend fun handle(
        e: Exception,
        input: FWAAssessmentExecutionInput,
        auth: AuthEntity,
        measurementLogger: MeasurementLogger
    ): NetworkDataResponse {
        val ctx = resolve(e)
        Log.e(TAG, "Handling ${e::class.simpleName} → [${ctx.errorCode}] ${ctx.userMessage}", e)
        return measurementLogger.handleException(
            input       = input,
            auth        = auth,
            e           = e,
            logEventName = ctx.errorCode,
            userMessage = ctx.userMessage,
            statusCode  = ctx.statusCode
        )
    }
}
