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
import com.elena.autoplanner.presentation.intents.TaskListIntent
import com.elena.autoplanner.presentation.navigation.MainNavigation
import com.elena.autoplanner.presentation.navigation.Screen
import com.elena.autoplanner.presentation.ui.components.BottomNavigationBar
import com.elena.autoplanner.presentation.ui.screens.more.MoreDrawerContent
import com.elena.autoplanner.presentation.ui.theme.AppTheme
import com.elena.autoplanner.presentation.viewmodel.TaskListViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

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
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        // --- Get the SINGLE ViewModel instance ---
        val listViewModel: TaskListViewModel = koinViewModel()

        // ... (Back Handler) ...

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                MoreDrawerContent(
                    drawerState = drawerState,
                    onNavigateToTasks = { listId, sectionId ->
                        scope.launch {
                            // This logic correctly uses the 'listViewModel' instance from MainApp
                            Log.d("MainActivity", "[ACTION] onNavigateToTasks triggered: listId=$listId, sectionId=$sectionId")
                            val intent = when {
                                listId != null && sectionId != null -> TaskListIntent.ViewSection(listId, sectionId)
                                listId != null -> TaskListIntent.ViewList(listId)
                                else -> TaskListIntent.ViewAllTasks
                            }
                            Log.d("MainActivity", "[ACTION] Sending intent to ViewModel: $intent")
                            listViewModel.sendIntent(intent)

                            Log.d("MainActivity", "[ACTION] Closing drawer")
                            drawerState.close()

                            val route = Screen.Tasks.createRoute(listId, sectionId)
                            Log.d("MainActivity", "[ACTION] Navigating to route: $route")
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            Log.d("MainActivity", "[ACTION] Navigation command sent")
                        }
                    }
                )
            }
        ) {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        navController = navController,
                        onMoreClick = { scope.launch { drawerState.open() } }
                    )
                }
            ) { innerPadding ->
                // --- Pass the SINGLE instance down ---
                MainNavigation(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                    taskListViewModel = listViewModel // <-- Pass the instance here
                )
            }
        }
    }
}
