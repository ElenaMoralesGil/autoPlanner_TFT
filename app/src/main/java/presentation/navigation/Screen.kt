package com.elena.autoplanner.presentation.navigation

sealed class Screen(val route: String) {

    object Tasks : Screen("tasks?listId={listId}&section={sectionId}") {
        fun createRoute(listId: Long? = null, sectionId: Long? = null): String {
            val listParam = listId?.let { "listId=$it" } ?: ""
            val sectionParam = sectionId?.let { "sectionId=$it" } ?: ""

            val params = listOfNotNull(
                listParam.takeIf { it.isNotEmpty() },
                sectionParam.takeIf { it.isNotEmpty() }
            ).joinToString("&") 
            return if (params.isEmpty()) "tasks" else "tasks?$params"
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
