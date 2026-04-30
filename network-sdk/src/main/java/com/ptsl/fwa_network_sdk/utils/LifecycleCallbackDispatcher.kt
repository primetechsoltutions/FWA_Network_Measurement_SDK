package com.ptsl.fwa_network_sdk.utils

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.ptsl.fwa_network_sdk.data_model.FWAMeasurementStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * Extracted from the main SDK facade to strictly handle the lifecycle validation
 * and safe callback dispatching to the main thread.
 */
internal class LifecycleCallbackDispatcher(
    private val activityRef: WeakReference<AppCompatActivity>?,
    private val lifecycleOwnerRef: WeakReference<LifecycleOwner>?
) {

    companion object {
        private const val TAG = "LifecycleDispatcher"
    }

    /**
     * Dispatches the callback safely on the Main thread, optimizing performance
     * by using Dispatchers.Main.immediate to avoid unnecessary thread context switching
     * if already on the main thread.
     */
    fun dispatch(
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
}
