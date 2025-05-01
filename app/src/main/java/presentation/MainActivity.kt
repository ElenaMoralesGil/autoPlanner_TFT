package com.elena.autoplanner.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.utils.DataSeeder
import com.elena.autoplanner.presentation.navigation.MainNavigation
import com.elena.autoplanner.presentation.navigation.Screen
import com.elena.autoplanner.presentation.ui.components.BottomNavigationBar
import com.elena.autoplanner.presentation.ui.screens.more.MoreDrawerContent
import com.elena.autoplanner.presentation.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val dataSeeder: DataSeeder by inject()
    private val taskRepository: TaskRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (dataSeeder.isEnabled()) {
            lifecycleScope.launch {
                val tasksResult = taskRepository.getTasks().first()

                val shouldSeed = tasksResult is TaskResult.Success && tasksResult.data.isEmpty()

                if (shouldSeed) {
                    Log.i("MainActivity", "No tasks found, running DataSeeder.")
                    dataSeeder.seedTasks(60)
                } else if (tasksResult is TaskResult.Success) {
                    Log.i(
                        "MainActivity",
                        "Tasks found (${tasksResult.data.size}), skipping DataSeeder."
                    )
                } else if (tasksResult is TaskResult.Error) {
                    Log.e(
                        "MainActivity",
                        "Error checking tasks for seeding: ${tasksResult.message}"
                    )
                }
            }
        }

        setContent {
            MainApp()
        }
    }
}

@Composable
fun MainApp() {
    AppTheme {
        val navController = rememberNavController()
        // --- Drawer State ---
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // --- Back Handler to close drawer ---
        if (drawerState.isOpen) {
            BackHandler {
                scope.launch { drawerState.close() }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen, // Only allow swipe-to-close when open
            drawerContent = {
                // --- Use the new Drawer Content composable ---
                MoreDrawerContent(
                    drawerState = drawerState,
                    onNavigateToTasks = { listId ->
                        // Navigation logic is now handled here or passed up
                        navController.navigate(Screen.Tasks.createRoute(listId)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        ) {
            // --- Main App Scaffold ---
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        navController = navController,
                        onMoreClick = { // Pass callback to open drawer
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }
            ) { innerPadding ->
                MainNavigation(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
