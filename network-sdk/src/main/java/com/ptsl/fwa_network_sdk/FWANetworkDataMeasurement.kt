package com.ptsl.fwa_network_sdk

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import com.ptsl.fwa_network_sdk.data_model.FWAMeasurementStatus
import com.ptsl.fwa_network_sdk.data_model.NetworkDataResponse
import com.ptsl.fwa_network_sdk.data_model.entity.AuthEntity
import com.ptsl.fwa_network_sdk.data_model.entity.FWAAssessmentExecutionInput
import com.ptsl.fwa_network_sdk.network_data_worker.FWADataMeasurementExecutor
import com.ptsl.fwa_network_sdk.network_data_worker.MeasurementLogger
import com.ptsl.fwa_network_sdk.utils.CheckPermissionHandler
import com.ptsl.fwa_network_sdk.utils.CommonUtils
import com.ptsl.fwa_network_sdk.utils.Constants
import com.ptsl.fwa_network_sdk.utils.LifecycleCallbackDispatcher
import com.ptsl.fwa_network_sdk.utils.SdkContainer
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference

/**
 * Main entry point for the Network Measurement SDK. 
 * Acts as a clean Facade pattern, delegating permission handling, lifecycle validation,
 * and data collection execution to respective dedicated classes.
 */
class FWANetworkDataMeasurement {
    private lateinit var checkPermissionHandler: CheckPermissionHandler
    private lateinit var context: Context
    private lateinit var applicationName: String
    private lateinit var callbackDispatcher: LifecycleCallbackDispatcher

    private val gson = Gson()

    companion object {
        private const val TAG = "FWANetworkDataMeasurement"
        private const val EXECUTION_TIMEOUT_MS = 60_000L
    }

    fun init(activity: AppCompatActivity, applicationName: String) {
        setup(activity, activity, CheckPermissionHandler(activity), applicationName)
    }

    fun init(fragment: Fragment, applicationName: String) {
        val activity = fragment.activity as? AppCompatActivity ?: return
        setup(activity, fragment, CheckPermissionHandler(fragment), applicationName)
    }

    private fun setup(
        activity: AppCompatActivity,
        owner: LifecycleOwner,
        permissionHandler: CheckPermissionHandler,
        appName: String
    ) {
        this.callbackDispatcher = LifecycleCallbackDispatcher(
            WeakReference(activity),
            WeakReference(owner)
        )
        this.checkPermissionHandler = permissionHandler
        this.context = activity.applicationContext
        this.applicationName = appName
        
        SdkContainer.init(this.context)
        Log.i(TAG, "SDK Initialized for $appName via ${owner::class.java.simpleName}")
    }

    /**
     * Starts the data collection and upload process.
     * @param msisdn User mobile number
     * @param integratedAppVersion Version of the host app
     * @param sdkInitiateTimeStamp Format: yyyy-MM-dd'T'HH:mm:ss
     * @param integratedAppEventName Unique name for the event
     * @param callback Result callback returning success status and details
     */
    fun startFWAMeasurement(
        msisdn: String,
        integratedAppVersion: String,
        sdkInitiateTimeStamp: String,
        integratedAppEventName: String,
        userLatitude: Double = 0.0,
        userLongitude: Double = 0.0,
        callback: (Boolean, FWAMeasurementStatus) -> Unit
    ) {
        if (!isInitialized()) {
            handleUninitializedError(callback)
            return
        }

        try {
            requestPermission { isGranted ->
                if (!isGranted) {
                    handlePermissionDenied(callback)
                } else {
                    performMeasurement(
                        msisdn,
                        integratedAppVersion,
                        sdkInitiateTimeStamp,
                        integratedAppEventName,
                        userLatitude,
                        userLongitude,
                        callback
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting upload process: ${e.message}")
            dispatchErrorCallback(callback, "Error starting upload process")
        }
    }

    private fun isInitialized(): Boolean {
        return this::checkPermissionHandler.isInitialized && 
               this::callbackDispatcher.isInitialized && 
               SdkContainer.isInitialized()
    }

    private fun handleUninitializedError(callback: (Boolean, FWAMeasurementStatus) -> Unit) {
        Log.e(TAG, "SDK not initialized. Call init() first.")
        val errorResponse = NetworkDataResponse(
            status = Constants.STATUS_FAILED,
            statusCode = 400,
            message = "SDK not initialized"
        )
        // Since callback dispatcher might not be initialized, dispatch directly
        callback(false, createMeasurementStatus(errorResponse, isSdkInit = false))
    }

    private fun handlePermissionDenied(callback: (Boolean, FWAMeasurementStatus) -> Unit) {
        Log.w(TAG, "Permissions not granted for measurement capture.")

        val isGpsEnabled = CommonUtils.isGpsEnabled(context)
        val isPermissionsGranted = checkPermissionHandler.isAllPermissionsGrantedExcludingGps()

        val errorMessage = when {
            !isPermissionsGranted -> Constants.ERR_MSG_PERMISSION_DENIED
            !isGpsEnabled -> Constants.ERR_MSG_GPS_DISABLED
            else -> "Required permissions are missing."
        }

        val error = NetworkDataResponse(
            status = Constants.STATUS_FAILED,
            testResult = Constants.RESULT_FAILED,
            statusCode = 400,
            message = errorMessage
        )
        callbackDispatcher.dispatch(callback, false, createMeasurementStatus(error))
    }

    private fun performMeasurement(
        msisdn: String,
        integratedAppVersion: String,
        sdkInitiateTimeStamp: String,
        integratedAppEventName: String,
        userLatitude: Double,
        userLongitude: Double,
        callback: (Boolean, FWAMeasurementStatus) -> Unit
    ) {
        SdkContainer.coroutineScope?.launch {
            val auth = createAuthEntity()
            // Persist auth via repository (single source of truth)
            SdkContainer.networkRepository?.saveAuth(auth)

            val input = FWAAssessmentExecutionInput(
                msisdn,
                integratedAppVersion,
                sdkInitiateTimeStamp,
                integratedAppEventName,
                userLatitude,
                userLongitude
            )

            val response = withTimeoutOrNull(EXECUTION_TIMEOUT_MS) {
                executeMeasurement(input)
            }

            if (response != null) {
                val isSuccess = response.status.equals(com.ptsl.fwa_network_sdk.utils.Constants.STATUS_SUCCESS, ignoreCase = true)
                callbackDispatcher.dispatch(callback, isSuccess, createMeasurementStatus(response))
            } else {
                dispatchErrorCallback(callback, "Assessment Failed")
            }
        }
    }

    private suspend fun executeMeasurement(input: FWAAssessmentExecutionInput): NetworkDataResponse? {
        val networkRepository   = SdkContainer.networkRepository   ?: return null
        val thresholdRepository = SdkContainer.thresholdRepository ?: return null
        val logRepository       = SdkContainer.logRepository       ?: return null
        val downloader          = SdkContainer.downloadUploadHelper ?: return null
        val networkProvider     = SdkContainer.networkStateProvider ?: return null

        val logger = MeasurementLogger(logRepository)

        return FWADataMeasurementExecutor(
            appContext          = context,
            networkRepository   = networkRepository,
            thresholdRepository = thresholdRepository,
            networkStateProvider = networkProvider,
            downloader          = downloader,
            measurementLogger   = logger
        ).execute(input)
    }

    private fun dispatchErrorCallback(
        callback: (Boolean, FWAMeasurementStatus) -> Unit,
        message: String
    ) {
        val errorResponse = NetworkDataResponse(
            status = com.ptsl.fwa_network_sdk.utils.Constants.STATUS_FAILED,
            statusCode = 400,
            message = message
        )
        callbackDispatcher.dispatch(callback, false, createMeasurementStatus(errorResponse))
    }

    private fun createAuthEntity() = AuthEntity(
        sdkVersion = BuildConfig.SdkVersion,
        isSdkInitialized = this::checkPermissionHandler.isInitialized,
        isLocationEnabled = com.ptsl.fwa_network_sdk.utils.CommonUtils.isLocationPermissionGranted(context) && com.ptsl.fwa_network_sdk.utils.CommonUtils.isGpsEnabled(context),
        isPhoneStateEnabled = com.ptsl.fwa_network_sdk.utils.CommonUtils.isPhoneStatePermissionGranted(context),
        hostAppName = applicationName
    )

    private fun createMeasurementStatus(
        networkDataResponse: NetworkDataResponse,
        isSdkInit: Boolean = this::checkPermissionHandler.isInitialized
    ): FWAMeasurementStatus {
        return FWAMeasurementStatus(
            isSdkInit = isSdkInit,
            isLocationEnabled = com.ptsl.fwa_network_sdk.utils.CommonUtils.isLocationPermissionGranted(context),
            isPhoneStateGranted = com.ptsl.fwa_network_sdk.utils.CommonUtils.isPhoneStatePermissionGranted(context),
            response = gson.toJson(networkDataResponse)
        )
    }

    private fun requestPermission(callback: (Boolean) -> Unit) {
        if (this::checkPermissionHandler.isInitialized) {
            if (checkPermissionHandler.isPermissionGranted()) {
                callback(true)
            } else {
                checkPermissionHandler.requestPermission(callback = callback)
            }
        } else {
            callback(false)
        }
    }
}