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
import com.ptsl.fwa_network_sdk.repository.LogRepository
import com.ptsl.fwa_network_sdk.repository.LogRepositoryImpl
import com.ptsl.fwa_network_sdk.repository.NetworkRepository
import com.ptsl.fwa_network_sdk.repository.NetworkRepositoryImpl
import com.ptsl.fwa_network_sdk.repository.ThresholdRepository
import com.ptsl.fwa_network_sdk.repository.ThresholdRepositoryImpl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Lightweight manual DI container for the SDK.
 *
 * Lifecycle:
 *  - [init] is called once from [com.ptsl.fwa_network_sdk.FWANetworkDataMeasurement.init].
 *  - All properties are lazily initialised and guarded by [isInitialized].
 *
 * Dependency graph (all `internal`):
 *
 *   ApiService  ─┬─► NetworkRepositoryImpl
 *   NetworkDao  ─┤
 *                ├─► LogRepositoryImpl
 *                └─► ThresholdRepositoryImpl
 */
internal object SdkContainer {
    private const val TAG = "SdkContainer"

    // ─── Core infrastructure ─────────────────────────────────────────────────

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

    // ─── Repositories ────────────────────────────────────────────────────────

    var networkRepository: NetworkRepository? = null
        private set
    var logRepository: LogRepository? = null
        private set
    var thresholdRepository: ThresholdRepository? = null
        private set

    // ─── State ───────────────────────────────────────────────────────────────

    @Volatile
    private var initialized = false

    fun isInitialized(): Boolean = initialized &&
            database            != null &&
            dao                 != null &&
            coroutineScope      != null &&
            apiService          != null &&
            downloadUploadHelper != null &&
            networkStateProvider != null &&
            networkRepository   != null &&
            logRepository       != null &&
            thresholdRepository != null

    // ─── Init ────────────────────────────────────────────────────────────────

    @Synchronized
    fun init(context: Context) {
        if (isInitialized()) {
            Log.i(TAG, "Already initialized, skipping re-initialization")
            return
        }

        try {
            val appContext = context.applicationContext

            // 1. Database
            val db = Room.databaseBuilder(
                appContext,
                NetworkDatabase::class.java,
                "network_db"
            ).fallbackToDestructiveMigration().build()
            database = db
            val currentDao = db.networkDao()
            dao = currentDao

            // 2. Coroutine scope
            val exceptionHandler = CoroutineExceptionHandler { _, exception ->
                Log.e(TAG, "Coroutine error: ${exception.message}")
            }
            coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

            // 3. Network layer
            val currentApiService = NetworkModule.apiService
            apiService            = currentApiService
            downloadUploadHelper  = DownloadUploadHelper(currentApiService)

            // 4. State providers
            networkStateProvider = NetworkStateProviderImpl(appContext)

            // 5. Repositories  ← single place where ApiService + Dao are wired together
            networkRepository   = NetworkRepositoryImpl(currentApiService, currentDao)
            logRepository       = LogRepositoryImpl(currentApiService, currentDao)
            thresholdRepository = ThresholdRepositoryImpl(currentApiService, currentDao)

            initialized = true
            Log.i(TAG, "SdkContainer initialized successfully")
        } catch (e: Exception) {
            initialized = false
            Log.e(TAG, "Init failed: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
}