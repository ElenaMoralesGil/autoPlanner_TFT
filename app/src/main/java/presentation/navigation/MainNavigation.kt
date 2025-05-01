package com.elena.autoplanner.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.elena.autoplanner.presentation.ui.screens.auth.LoginScreen
import com.elena.autoplanner.presentation.ui.screens.auth.RegisterScreen
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarScreen
import com.elena.autoplanner.presentation.ui.screens.more.MoreScreen
import com.elena.autoplanner.presentation.ui.screens.profile.EditProfileScreen
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
        composable(
            route = Screen.Tasks.route,
            arguments = listOf(navArgument("listId") {
                type = NavType.StringType // Read as String
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            // ViewModel will get listId via SavedStateHandle automatically
            TasksScreen(
                onNavigateToPlanner = { navController.navigate(Screen.Planner.route) },
                // This callback might not be needed if ViewModel handles navigation state
                onNavigateToList = { listId ->
                    navController.navigate(Screen.Tasks.createRoute(listId)) {
                        // Optional: Configure popUpTo behavior if needed
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Calendar.route) {
            CalendarScreen()
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                }
            )
        }
        composable(Screen.More.route) {
            MoreScreen(
                onNavigateToTasks = { listId ->
                    // Navigate to Tasks screen, passing the listId
                    navController.navigate(Screen.Tasks.createRoute(listId)) {
                        // Pop up to the start destination of the graph to avoid building up backstack.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously visited item
                        restoreState = true
                    }
                }
            )
        }
        composable(Screen.Planner.route) {
            AutoPlannerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Navigate back to Profile or Tasks after login
                    navController.popBackStack(Screen.Profile.route, inclusive = false)
                    // Or navigate to a specific screen: navController.navigate(Screen.Tasks.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    // Navigate back to Profile or Tasks after registration
                    navController.popBackStack(Screen.Profile.route, inclusive = false)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onReAuthenticationNeeded = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.EditProfile.route) { inclusive = true }
                    }
                }
            )
        }

    }
}

