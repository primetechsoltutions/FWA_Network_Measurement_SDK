package com.ptsl.fwa_network_sdk.utils


import android.os.Build
import com.ptsl.fwa_network_sdk.data_model.logger.EventLogModel

object NetworkEventLogger {

    fun createNetworkRequestFailedLog(
        hostAppName: String,
        eventName: String,
        errorMessage: String,
        stackTrace: String? = null,
        statusCode: Int = 0
    ): EventLogModel {
        return EventLogModel(
            logSource = "$hostAppName: $eventName",
            eventType = "Error",
            title = "Network Request Failed",
            description = "Failed to post network data",
            statusCode = statusCode,
            status = if (statusCode in 400..599) "HTTP Error" else "System Error",
            message = errorMessage,
            stackTrace = stackTrace ?: "No stack trace available",
            os = Build.VERSION.SDK_INT.toString(),
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
    }

    fun createNetworkDataFetchFailedLog(
        hostAppName: String,
        eventName: String,
        exceptionMessage: String,
        stackTrace: String,
        statusCode: Int = 902
    ): EventLogModel {
        return EventLogModel(
            logSource = "$hostAppName: $eventName",
            eventType = "Error",
            title = "Get Network Request Failed From Exception",
            description = "Failed to get network data",
            statusCode = statusCode,
            status = "False",
            message = exceptionMessage,
            stackTrace = stackTrace,
            os = Build.VERSION.SDK_INT.toString(),
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
    }

    fun createPermissionMissingLog(
        hostAppName: String,
        eventName: String,
        exceptionMessage: String,
        stackTrace: String?,
        statusCode: Int = 901
    ): EventLogModel {
        return EventLogModel(
            logSource = "$hostAppName: $eventName",
            eventType = "Error",
            title = "Get Network Request Failed",
            description = "Failed to get network data due to missing permissions",
            statusCode = statusCode,
            status = "False",
            message = exceptionMessage,
            stackTrace = stackTrace ?: "No stack trace available",
            os = Build.VERSION.SDK_INT.toString(),
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
    }
}
