package com.elena.autoplanner.presentation.navigation

sealed class Screen(val route: String) {
    // Corrected route definition with '&'
    object Tasks : Screen("tasks?listId={listId}&section={sectionId}") {
        fun createRoute(listId: Long? = null, sectionId: Long? = null): String {
            val listParam = listId?.let { "listId=$it" } ?: ""
            val sectionParam = sectionId?.let { "sectionId=$it" } ?: ""
            // Use filterNotNull and joinToString for cleaner parameter handling
            val params = listOfNotNull(
                listParam.takeIf { it.isNotEmpty() },
                sectionParam.takeIf { it.isNotEmpty() }
            ).joinToString("&") // This already uses '&' correctly
            return if (params.isEmpty()) "tasks" else "tasks?$params"
        }

        const val routeBase = "tasks" // Keep for bottom nav matching
    }

    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
    object Planner : Screen("planner")
    object Login : Screen("login")
    object Register : Screen("register")
    object EditProfile : Screen("edit_profile")
}
