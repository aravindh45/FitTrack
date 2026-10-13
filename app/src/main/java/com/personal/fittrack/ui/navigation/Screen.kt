package com.personal.fittrack.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object LogFood : Screen("log_food")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object EditProfile : Screen("edit_profile")
}
