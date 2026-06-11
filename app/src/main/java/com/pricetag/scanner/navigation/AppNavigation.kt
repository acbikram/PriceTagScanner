package com.pricetag.scanner.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pricetag.scanner.presentation.history.HistoryScreen
import com.pricetag.scanner.presentation.main.MainScreen
import com.pricetag.scanner.presentation.settings.SettingsScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController  = navController,
        startDestination = Screen.Main.route,
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateHistory  = { navController.navigate(Screen.History.route) },
                onNavigateSettings = { navController.navigate(Screen.Settings.route) },
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
