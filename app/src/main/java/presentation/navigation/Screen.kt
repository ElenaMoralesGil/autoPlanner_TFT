package com.elena.autoplanner.presentation.navigation

sealed class Screen(val route: String) {
    object Tasks : Screen("tasks?listId={listId}") {
        fun createRoute(listId: Long? = null): String {
            return if (listId != null) "tasks?listId=$listId" else "tasks"
        }
        const val routeBase = "tasks"
    }
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
    object Planner : Screen("planner")
    object Login : Screen("login")
    object Register : Screen("register")
    object EditProfile : Screen("edit_profile")
}