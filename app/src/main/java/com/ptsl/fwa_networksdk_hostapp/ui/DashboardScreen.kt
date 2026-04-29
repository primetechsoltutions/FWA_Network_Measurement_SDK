package com.ptsl.fwa_networksdk_hostapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ptsl.fwa_networksdk_hostapp.ui.theme.*

@Composable
fun DashboardScreen(
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToFragmentAnalysis: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp)
    ) {
        Text(
            text = "FWA Assessment",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Monitor your network health",
            color = TextDim,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            DashboardCard(
                title = "Speed Test",
                icon = Icons.Default.Refresh,
                color = PrimaryBlue,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSpeedTest
            )
            Spacer(modifier = Modifier.width(16.dp))
            DashboardCard(
                title = "Speed Test Fragment",
                icon = Icons.Default.PlayArrow,
                color = AccentPurple,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToFragmentAnalysis
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            DashboardCard(
                title = "Support",
                icon = Icons.Default.Person,
                color = SuccessGreen,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSupport
            )
            Spacer(modifier = Modifier.width(16.dp))
            DashboardCard(
                title = "Settings",
                icon = Icons.Default.Settings,
                color = Color.Gray,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BgDarkCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

