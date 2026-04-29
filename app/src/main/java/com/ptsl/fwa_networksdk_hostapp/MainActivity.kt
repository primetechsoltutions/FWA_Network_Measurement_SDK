package com.ptsl.fwa_networksdk_hostapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.rememberNavController
import com.ptsl.fwa_network_sdk.FWANetworkDataMeasurement
import com.ptsl.fwa_networksdk_hostapp.ui.AppNavigation
import com.ptsl.fwa_networksdk_hostapp.ui.theme.FWASDKTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val sdk = FWANetworkDataMeasurement()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sdk.init(this, "MyBL")

        setContent {
            FWASDKTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    onNavigateToFragment = {
                        startActivity(android.content.Intent(this, com.ptsl.fwa_networksdk_hostapp.ui.FragmentHostActivity::class.java))
                    },
                    onStartAssessment = { onComplete ->

                        val timeStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
                        sdk.startFWAMeasurement(
                            msisdn = "8801900000000",
                            integratedAppVersion = "1.1.0",
                            sdkInitiateTimeStamp = timeStamp,
                            integratedAppEventName = "App_Main_Diagnostic"
                        ) { success, status ->
                            onComplete(success, status.response)
                        }
                    }
                )
            }
        }
    }
}


