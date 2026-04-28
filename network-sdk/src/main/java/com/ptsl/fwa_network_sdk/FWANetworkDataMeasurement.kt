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
import com.ptsl.fwa_network_sdk.network_data_worker.FWAAssessmentExecutionInput
import com.ptsl.fwa_network_sdk.network_data_worker.FWADataMeasurementExecutor
import com.ptsl.fwa_network_sdk.utils.CheckPermissionHandler
import com.ptsl.fwa_network_sdk.utils.SdkContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference

/**
 * Main entry point for the Network Measurement SDK. Handles initialization, permission requests,
 * and enqueueing measurement tasks.
 */
class FWANetworkDataMeasurement {
    private var activityRef: WeakReference<AppCompatActivity>? = null
    private var lifecycleOwnerRef: WeakReference<LifecycleOwner>? = null
    private lateinit var checkPermissionHandler: CheckPermissionHandler
    private lateinit var context: Context
    private lateinit var applicationName: String

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
        this.activityRef = WeakReference(activity)
        this.lifecycleOwnerRef = WeakReference(owner)
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
        return this::checkPermissionHandler.isInitialized && SdkContainer.isInitialized()
    }

    private fun handleUninitializedError(callback: (Boolean, FWAMeasurementStatus) -> Unit) {
        Log.e(TAG, "SDK not initialized. Call init() first.")
        val errorResponse = NetworkDataResponse(
            status = "Failed",
            statusCode = 400,
            message = "SDK not initialized"
        )
        callback(false, createMeasurementStatus(errorResponse, isSdkInit = false))
    }

    private fun handlePermissionDenied(callback: (Boolean, FWAMeasurementStatus) -> Unit) {
        Log.w(TAG, "Permissions not granted for measurement capture.")

        val isGpsEnabled = checkPermissionHandler.isGpsEnabled()
        val isPermissionsGranted = checkPermissionHandler.isAllPermissionsGrantedExcludingGps()

        val errorMessage = when {
            !isPermissionsGranted -> "Required permissions (Location or Phone State) are missing."
            !isGpsEnabled -> "GPS is disabled. Please enable GPS to proceed."
            else -> "Required permissions are missing."
        }

        val error = NetworkDataResponse(
            status = "Failed",
            testResult = "Failed",
            statusCode = 400,
            message = errorMessage
        )
        dispatchCallback(callback, false, createMeasurementStatus(error))
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
            SdkContainer.dao?.insertAuthData(auth)

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
                dispatchCallback(
                    callback,
                    response.status.equals("Success", ignoreCase = true),
                    createMeasurementStatus(response)
                )
            } else {
                dispatchErrorCallback(callback, "Assessment Failed")
            }
        }
    }

    private suspend fun executeMeasurement(input: FWAAssessmentExecutionInput): NetworkDataResponse? {
        val apiService = SdkContainer.apiService ?: return null
        val downloader = SdkContainer.downloadUploadHelper ?: return null
        val dao = SdkContainer.dao ?: return null
        val networkProvider = SdkContainer.networkStateProvider ?: return null
        val thresholdManager = SdkContainer.thresholdManager ?: return null

        return FWADataMeasurementExecutor(
            context,
            apiService,
            downloader,
            dao,
            networkProvider,
            thresholdManager
        ).execute(input)
    }


    private fun dispatchErrorCallback(
        callback: (Boolean, FWAMeasurementStatus) -> Unit,
        message: String
    ) {
        val errorResponse = NetworkDataResponse(
            status = "Failed",
            statusCode = 400,
            message = message
        )
        dispatchCallback(callback, false, createMeasurementStatus(errorResponse))
    }

    private fun dispatchCallback(
        callback: (Boolean, FWAMeasurementStatus) -> Unit,
        success: Boolean,
        status: FWAMeasurementStatus
    ) {
        SdkContainer.coroutineScope?.launch {
            withContext(Dispatchers.Main) {
                if (isLifecycleOwnerValid()) {
                    callback(success, status)
                }
            }
        }
    }

    private fun isLifecycleOwnerValid(): Boolean {
        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "Host Activity is no longer valid. Skipping callback.")
            return false
        }

        val owner = lifecycleOwnerRef?.get() ?: run {
            Log.w(TAG, "LifecycleOwner reference lost. Skipping callback.")
            return false
        }

        if (owner is Fragment) {
            if (!owner.isAdded || owner.isDetached || owner.viewLifecycleOwnerLiveData.value == null) {
                Log.w(TAG, "Host Fragment is no longer valid (detached or removed). Skipping callback.")
                return false
            }
        }
        return true
    }

    private fun createAuthEntity() = AuthEntity(
        sdkVersion = BuildConfig.SdkVersion,
        isSdkInitialized = this::checkPermissionHandler.isInitialized,
        isLocationEnabled = checkPermissionHandler.isLocationPermissionGranted() && checkPermissionHandler.isGpsEnabled(),
        isPhoneStateEnabled = checkPermissionHandler.isPhoneStatePermissionGranted(),
        hostAppName = applicationName
    )

    private fun createMeasurementStatus(
        networkDataResponse: NetworkDataResponse,
        isSdkInit: Boolean = this::checkPermissionHandler.isInitialized
    ): FWAMeasurementStatus {
        return FWAMeasurementStatus(
            isSdkInit = isSdkInit,
            isLocationEnabled = checkPermissionHandler.isLocationPermissionGranted(),
            isPhoneStateGranted = checkPermissionHandler.isPhoneStatePermissionGranted(),
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