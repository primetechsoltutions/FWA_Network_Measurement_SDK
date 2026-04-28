package com.ptsl.networksdk_event.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object SpeedTest : Screen("speed_test")
    object Game : Screen("game")
    object Support : Screen("support")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    onNavigateToFragment: () -> Unit,
    onStartAssessment: ((Boolean, String?) -> Unit) -> Unit
) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToSpeedTest = { navController.navigate(Screen.SpeedTest.route) },
                onNavigateToFragmentAnalysis = onNavigateToFragment,
                onNavigateToSupport = { navController.navigate(Screen.Support.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.SpeedTest.route) {
            SpeedTestScreen(
                onBack = { navController.popBackStack() },
                onStartAssessment = onStartAssessment
            )
        }
        composable(Screen.Game.route) {
            PlaceholderScreen("Gaming", onBack = { navController.popBackStack() })
        }
        composable(Screen.Support.route) {
            PlaceholderScreen("Support", onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            PlaceholderScreen("Settings", onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.ptsl.networksdk_event.ui.theme.BgDarkCard,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.ptsl.networksdk_event.ui.theme.BgDark)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Coming Soon: $title", color = com.ptsl.networksdk_event.ui.theme.TextDim)
        }
    }
}

