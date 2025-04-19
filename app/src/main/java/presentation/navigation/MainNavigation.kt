package com.elena.autoplanner.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.elena.autoplanner.presentation.ui.screens.auth.LoginScreen
import com.elena.autoplanner.presentation.ui.screens.auth.RegisterScreen
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
            ProfileScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                // Add onNavigateToEditProfile later
            )
        }
        composable(Screen.More.route) {
            MoreScreen()
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

    }
}

