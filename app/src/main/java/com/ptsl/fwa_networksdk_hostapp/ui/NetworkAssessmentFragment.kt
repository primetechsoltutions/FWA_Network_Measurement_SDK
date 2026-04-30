package com.ptsl.fwa_networksdk_hostapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.ptsl.fwa_network_sdk.FWANetworkDataMeasurement
import com.ptsl.fwa_networksdk_hostapp.ui.theme.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class NetworkAssessmentFragment : Fragment() {

    private val sdk = FWANetworkDataMeasurement()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sdk.init(this, "MyBL")

        return ComposeView(requireContext()).apply {
            setContent {
                FWASDKTheme {
                    AssessmentFragmentScreen(
                        onBack = { parentFragmentManager.popBackStack() },
                        onRunAssessment = { onResult ->
                            val timeStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
                            sdk.startFWAMeasurement(
                                msisdn = "8801900000000",
                                integratedAppVersion = "1.0.0",
                                sdkInitiateTimeStamp = timeStamp,
                                integratedAppEventName = "Fragment_Detailed_Analysis"
                            ) { success, status ->
                                onResult(success, status.response)
                            }
                        }
                    )
                }
            }
        }
    }
}

data class AssessmentResult(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val downloadSpeed: Double,
    val uploadSpeed: Double,
    val rsrp: Int,
    val rsrq: Int,
    val snr: Int,
    val status: String,
    val testResult: String,
    val message: String,
    val rawJson: String
)

@Composable
fun AssessmentFragmentScreen(
    onBack: () -> Unit,
    onRunAssessment: ((Boolean, String?) -> Unit) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var selectedResult by remember { mutableStateOf<AssessmentResult?>(null) }
    val resultsHistory = remember { mutableStateListOf<AssessmentResult>() }

    // Current test state
    var currentDlSpeed by remember { mutableStateOf(0.0) }
    var currentUlSpeed by remember { mutableStateOf(0.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Fragment Speed Test",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Modern Speed Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BgDarkCard),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "CURRENT MEASUREMENT", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpeedMetric(label = "Download", value = currentDlSpeed, color = PrimaryBlue)
                    VerticalDivider(modifier = Modifier.height(60.dp).width(1.dp), color = Color.White.copy(alpha = 0.1f))
                    SpeedMetric(label = "Upload", value = currentUlSpeed, color = AccentPurple)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isLoading = true
                        onRunAssessment { success, response ->
                            isLoading = false
                            response?.let { jsonStr ->
                                try {
                                    val json = JSONObject(jsonStr)
                                    val data = json.optJSONObject("data")
                                    val speedPair = data?.optJSONObject("speedPair")
                                    val networkData = data?.optJSONObject("networkData")

                                    val dl = speedPair?.optDouble("dlSpeedKbps", 0.0)?.div(1000.0) ?: 0.0
                                    val ul = speedPair?.optDouble("ulSpeedKbps", 0.0)?.div(1000.0) ?: 0.0
                                    
                                    currentDlSpeed = dl
                                    currentUlSpeed = ul

                                    val newResult = AssessmentResult(
                                        timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                                        downloadSpeed = dl,
                                        uploadSpeed = ul,
                                        rsrp = networkData?.optInt("RSRP", 0) ?: 0,
                                        rsrq = networkData?.optInt("RSRQ", 0) ?: 0,
                                        snr = networkData?.optInt("SNR", 0) ?: 0,
                                        status = json.optString("status", "Unknown"),
                                        testResult = json.optString("testResult", "Unknown"),
                                        message = json.optString("message", ""),
                                        rawJson = jsonStr
                                    )
                                    resultsHistory.add(0, newResult)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("RUN SCAN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History List
        Text(
            text = "TEST HISTORY",
            color = TextDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(resultsHistory) { result ->
                HistoryItem(result = result, onClick = { selectedResult = result })
            }
        }
    }

    // Detail View Modal
    selectedResult?.let { result ->
        DetailDialog(result = result, onDismiss = { selectedResult = null })
    }
}

@Composable
fun SpeedMetric(label: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextDim, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = String.format("%.1f", value),
                color = color,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Mbps",
                color = TextDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun HistoryItem(result: AssessmentResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = BgDarkCard.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Scan at ${result.timestamp}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "DL: ${String.format("%.1f", result.downloadSpeed)} | UL: ${String.format("%.1f", result.uploadSpeed)}", color = TextDim, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextDim)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailDialog(result: AssessmentResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BgDark,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Test Details", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Detailed Cards
                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailCard(label = "RSRP", value = "${result.rsrp} dBm", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(12.dp))
                    DetailCard(label = "RSRQ", value = "${result.rsrq} dB", modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailCard(label = "SNR", value = "${result.snr} dB", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(12.dp))
                    DetailCard(label = "Status", value = result.status, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailCard(label = "Test Result", value = result.testResult, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(12.dp))
                    DetailCard(label = "Message", value = result.message, modifier = Modifier.weight(1f))
                }


                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "RAW JSON RESPONSE", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text(text = result.rawJson, color = TextDim, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BgDarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, color = TextDim, fontSize = 12.sp)
            Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}



