package com.ptsl.fwa_networksdk_hostapp.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ptsl.fwa_networksdk_hostapp.model.RecentTest
import com.ptsl.fwa_networksdk_hostapp.ui.theme.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SpeedTestScreen(
    onBack: () -> Unit,
    onStartAssessment: ((Boolean, String?) -> Unit) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var resultJson by remember { mutableStateOf("") }
    var mainSpeed by remember { mutableStateOf("0.0") }
    var testRes by remember { mutableStateOf("") }
    val recentTests = remember { mutableStateListOf<RecentTest>() }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Network Speed Test",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BgDarkCard)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Column {
                     Row {
                         Column {
                             Text(text = "Ready for scan", color = TextDim, fontSize = 14.sp)
                             Text(text = "Your Download Speed : ", color = TextDim, fontSize = 14.sp)
                         }
                         Spacer(modifier = Modifier.width(16.dp))
                         Card(
                             colors = CardDefaults.cardColors(containerColor = BgDarkCard),
                             shape = RoundedCornerShape(16.dp)
                         ){
                             Text(text = "Test Result: ${testRes}", color = TextDim, fontSize = 14.sp)
                         }
                     }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = mainSpeed,
                                color = PrimaryBlue,
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mbps",
                                color = TextDim,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            showResult = false
                            onStartAssessment { success, response ->
                                isLoading = false
                                response?.let { jsonStr ->
                                    resultJson = jsonStr
                                    showResult = true
                                    try {
                                        val json = JSONObject(jsonStr)
                                        val data = json.optJSONObject("data")
                                        val speedPair = data?.optJSONObject("speedPair")
                                        val networkData = data?.optJSONObject("networkData")

                                        val dl = (speedPair?.optDouble("dlSpeedKbps") ?: 0.0) / 1000.0
                                        val ul = (speedPair?.optDouble("ulSpeedKbps") ?: 0.0) / 1000.0
                                        val rsrp = networkData?.optInt("RSRP") ?: 0
                                        val status = json.optString("status", "Failed")
                                       val testResult = json.optString("testResult", "Unknown")
                                        mainSpeed = String.format("%.1f", dl)
                                        testRes=testResult

                                        val newTest = RecentTest(
                                            timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date()),
                                            downloadSpeed = dl,
                                            uploadSpeed = ul,
                                            rsrp = rsrp,
                                            status = status,
                                            testResult = testResult
                                        )
                                        recentTests.add(0, newTest)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Run Diagnostic")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Tests Label
            Text(
                text = "Recent Tests",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Recent Tests List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(recentTests) { test ->
                    RecentTestItem(test)
                }
            }
        }

        // Loading Overlay
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LoadingOverlay),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        }

        // Result Card Overlay
        AnimatedVisibility(
            visible = showResult,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(max = 400.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BgDarkCard)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Assessment Result",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(text = resultJson, color = TextDim, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showResult = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTestItem(test: RecentTest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = test.timestamp, color = TextDim, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${String.format("%.1f", test.downloadSpeed)} Mbps",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${String.format("%.1f", test.uploadSpeed)} Mbps",
                        color = AccentPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "RSRP: ${test.rsrp} dBm", color = TextDim, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (test.status.equals("Success", true)) SuccessGreen else ErrorRed,
                            CircleShape
                        )
                )
            }
        }
    }
}

