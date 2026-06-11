package com.pricetag.scanner.navigation

sealed class Screen(val route: String) {
    object Main     : Screen("main")
    object History  : Screen("history")
    object Settings : Screen("settings")
}
