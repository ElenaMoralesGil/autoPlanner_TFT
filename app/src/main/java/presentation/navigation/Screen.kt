package presentation.navigation

sealed class Screen(val route: String) {
    object Tasks : Screen("tasks")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
    object More : Screen("more")
}