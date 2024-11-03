package presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import presentation.ui.screens.tasks.TasksScreen
import presentation.ui.screens.calendar.CalendarScreen
import presentation.ui.screens.profile.ProfileScreen
import presentation.ui.screens.more.MoreScreen

@Composable
fun MainNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "tasks",
        modifier = modifier
    ) {
        composable("tasks") { TasksScreen() }
        composable("calendar") { CalendarScreen() }
        composable("profile") { ProfileScreen() }
        composable("more") { MoreScreen() }
    }
}
