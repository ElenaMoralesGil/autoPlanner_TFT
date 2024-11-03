package com.elena.autoplanner.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import presentation.ui.components.BottomNavigationBar
import presentation.ui.screens.tasks.TasksScreen
import presentation.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainApp()
        }
    }
}

@Composable
fun MainApp() {
    AppTheme {
        val navController = rememberNavController()
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route

        Scaffold(
            bottomBar = {
                BottomNavigationBar(navController = navController, currentRoute = currentRoute)
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "tasks"
            ) {
                composable("tasks") {
                    TasksScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                // Additional routes can be added here (e.g., "calendar", "profile")
            }
        }
    }
}
