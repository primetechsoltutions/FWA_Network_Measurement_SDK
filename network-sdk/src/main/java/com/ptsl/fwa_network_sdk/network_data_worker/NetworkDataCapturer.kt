package com.ptsl.fwa_network_sdk.network_data_worker

import android.content.Context
import android.util.Log
import com.ptsl.fwa_network_sdk.data_model.entity.FTPNetworkDataEntity
import com.ptsl.fwa_network_sdk.dl_ul_test.DownloadUploadHelper
import com.ptsl.fwa_network_sdk.provider.NetworkStateProvider
import com.ptsl.fwa_network_sdk.utils.Constants
import com.ptsl.fwa_network_sdk.utils.prepareFTPData
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.cell.ICell
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection

/**
 * Handles the responsibility of capturing raw network cell data via NetMonster.
 * Optimised to cleanly separate radio measurement logic from the main executor.
 */
internal class NetworkDataCapturer(
    private val appContext: Context,
    private val networkStateProvider: NetworkStateProvider,
    private val downloader: DownloadUploadHelper
) {
    companion object {
        private const val TAG = "NetworkDataCapturer"
    }

    suspend fun capture(locationPair: Pair<Double, Double>): FTPNetworkDataEntity {
        val isMobileConnected = networkStateProvider.isMobileNetworkConnected()
        val activeMnc = if (isMobileConnected) networkStateProvider.getActiveNetworkMNC() else "-1"
        val activeMncClean = activeMnc.removePrefix("0")

        Log.d(TAG, "capture: activeMnc=$activeMnc, isMobileConnected=$isMobileConnected")

        if (isMobileConnected && activeMncClean != Constants.BANGLALINK_MNC) {
            Log.w(TAG, "Active MNC mismatch: expected ${Constants.BANGLALINK_MNC}, got $activeMnc")
            return FTPNetworkDataEntity().apply { technologyType = Constants.TECH_SKIP_MNC_MISMATCH }
        }

        val cells = fetchCells()
        if (cells.isNullOrEmpty()) {
            Log.w(TAG, "No cells detected by NetMonster")
            return FTPNetworkDataEntity()
        }

        return processCells(cells, locationPair, isMobileConnected, activeMnc)
    }

    private fun fetchCells(): List<ICell>? {
        return try {
            if (networkStateProvider.hasLocationPermissions()) {
                NetMonsterFactory.get(appContext).getCells()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cells: ${e.message}")
            null
        }
    }

    private suspend fun processCells(
        cells: List<ICell>,
        locationPair: Pair<Double, Double>,
        isMobileConnected: Boolean,
        activeMnc: String
    ): FTPNetworkDataEntity {
        var foundNon4gBanglalink = false

        for (cell in cells) {
            if (cell.connectionStatus is PrimaryConnection) {
                val cellMnc = cell.network?.mnc?.removePrefix("0") ?: ""
                val isBanglalink = cellMnc == Constants.BANGLALINK_MNC
                val isLte = cell is cz.mroczis.netmonster.core.model.cell.CellLte

                Log.d(TAG, "Inspecting Primary Cell: type=${cell.javaClass.simpleName}, mnc=$cellMnc, isLte=$isLte, isBL=$isBanglalink")

                if (isBanglalink) {
                    if (isLte) {
                        Log.i(TAG, "✅ Found valid Banglalink 4G Primary Cell")
                        return cell.prepareFTPData(locationPair, downloader, isMobileConnected, activeMnc)
                    } else {
                        Log.d(TAG, "Found Banglalink Primary cell but it is NOT 4G (Technology: ${cell.javaClass.simpleName})")
                        foundNon4gBanglalink = true
                    }
                }
            }
        }

        return if (foundNon4gBanglalink) {
            Log.w(TAG, "❌ No Banglalink 4G cell found, only lower technologies detected")
            FTPNetworkDataEntity().apply { technologyType = Constants.TECH_NON_4G_IGNORED }
        } else {
            Log.w(TAG, "❌ No Banglalink primary connection detected in cell list")
            FTPNetworkDataEntity()
        }
    }
}
