package com.ptsl.fwa_network_sdk.utils

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.ptsl.fwa_network_sdk.api.ApiService
import com.ptsl.fwa_network_sdk.api.NetworkModule
import com.ptsl.fwa_network_sdk.db.NetworkDao
import com.ptsl.fwa_network_sdk.db.NetworkDatabase
import com.ptsl.fwa_network_sdk.dl_ul_test.DownloadUploadHelper
import com.ptsl.fwa_network_sdk.provider.NetworkStateProvider
import com.ptsl.fwa_network_sdk.provider.NetworkStateProviderImpl
import com.ptsl.fwa_network_sdk.provider.ThresholdManager
import com.ptsl.fwa_network_sdk.provider.ThresholdManagerImpl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal object SdkContainer {
    // Encapsulation: Using `private set` prevents other classes from accidentally mutating the
    // global state of the SDK after initialization. It can only be read externally.
    var database: NetworkDatabase? = null
        private set
    var dao: NetworkDao? = null
        private set
    var coroutineScope: CoroutineScope? = null
        private set
    var apiService: ApiService? = null
        private set
    var downloadUploadHelper: DownloadUploadHelper? = null
        private set
    var networkStateProvider: NetworkStateProvider? = null
        private set
    var thresholdManager: ThresholdManager? = null
        private set

    @Volatile
    private var initialized = false

    fun isInitialized(): Boolean = initialized &&
            database != null &&
            dao != null &&
            coroutineScope != null &&
            apiService != null &&
            downloadUploadHelper != null &&
            networkStateProvider != null &&
            thresholdManager != null

    @Synchronized
    fun init(context: Context) {
        if (isInitialized()) {
            Log.i("SdkContainer", "Already initialized, skipping re-initialization")
            return
        }

        try {
            val appContext = context.applicationContext
            database = Room.databaseBuilder(
                appContext,
                NetworkDatabase::class.java,
                "network_db"
            ).fallbackToDestructiveMigration().build()
            
            val currentDao = database?.networkDao()
            dao = currentDao

            val exceptionHandler = CoroutineExceptionHandler { _, exception ->
                Log.e("SdkContainer", "Coroutine error: ${exception.message}")
            }

            coroutineScope = CoroutineScope(
                SupervisorJob() + Dispatchers.IO + exceptionHandler
            )
            
            val currentApiService = NetworkModule.apiService
            apiService = currentApiService
            downloadUploadHelper = currentApiService?.let { DownloadUploadHelper(it) }
            
            networkStateProvider = NetworkStateProviderImpl(appContext)
            
            if (currentApiService != null && currentDao != null) {
                thresholdManager = ThresholdManagerImpl(currentApiService, currentDao)
            }

            initialized = true
            Log.i("SdkContainer", "SdkContainer initialized successfully")
        } catch (e: Exception) {
            initialized = false
            Log.e("SdkContainer", "Init failed: ${e.message}")
        }
    }
}