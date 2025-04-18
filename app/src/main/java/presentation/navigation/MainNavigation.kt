package com.elena.autoplanner.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarScreen
import com.elena.autoplanner.presentation.ui.screens.more.MoreScreen
import com.elena.autoplanner.presentation.ui.screens.profile.ProfileScreen
import com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen.TasksScreen
import com.elena.autoplanner.presentation.ui.screens.tasks.planner.AutoPlannerScreen

@Composable
fun MainNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Tasks.route,
        modifier = modifier
    ) {
        composable(Screen.Tasks.route) {
            TasksScreen(
                onNavigateToPlanner = {
                    navController.navigate(Screen.Planner.route)
                }
            )
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
        composable(Screen.Planner.route) {
            AutoPlannerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
