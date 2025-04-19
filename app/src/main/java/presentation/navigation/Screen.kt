package com.elena.autoplanner.presentation.navigation

sealed class Screen(val route: String) {
    object Tasks : Screen("tasks")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
    object More : Screen("more")
    object Planner : Screen("planner")
    object Login : Screen("login")
    object Register : Screen("register")
}