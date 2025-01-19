package presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import presentation.ui.screens.calendar.CalendarScreen
import presentation.ui.screens.more.MoreScreen
import presentation.ui.screens.profile.ProfileScreen
import presentation.ui.screens.tasks.TasksScreen

@Composable
fun MainNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Tasks.route,
        modifier = modifier
    ) {
        composable(Screen.Tasks.route) {
            TasksScreen(navController = navController)
        }
        composable(Screen.Calendar.route) {
            CalendarScreen()
        }
        composable(Screen.Profile.route) {
            ProfileScreen()
        }
        composable(Screen.More.route) {
            MoreScreen()
        }
    }
}
