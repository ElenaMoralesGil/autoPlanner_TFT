package com.elena.autoplanner.presentation

import android.Manifest
import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.utils.DataSeeder
import com.elena.autoplanner.presentation.effects.MoreEffect
import com.elena.autoplanner.presentation.intents.TaskListIntent
import com.elena.autoplanner.presentation.navigation.MainNavigation
import com.elena.autoplanner.presentation.navigation.Screen
import com.elena.autoplanner.presentation.ui.components.BottomNavigationBar
import com.elena.autoplanner.presentation.ui.screens.more.MoreDrawerContent
import com.elena.autoplanner.presentation.ui.theme.AppTheme
import com.elena.autoplanner.presentation.viewmodel.MoreViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskListViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    private val dataSeeder: DataSeeder by inject()
    private val taskRepository: TaskRepository by inject()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("MainActivity", "POST_NOTIFICATIONS permission granted.")
                // You might trigger a refresh of alarms here if needed
            } else {
                Log.w("MainActivity", "POST_NOTIFICATIONS permission denied.")
                // Explain to the user why the permission is needed (e.g., via Snackbar or Dialog)
                // Show snackbar: scope.launch { snackbarHostState.showSnackbar("Notifications disabled. Reminders won't work.") }
            }
        }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "POST_NOTIFICATIONS permission already granted.")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.w("MainActivity", "Showing rationale for POST_NOTIFICATIONS.")
                    // Show an explanation to the user *asynchronously* before requesting again.
                    // For now, just request again or show snackbar.
                    // scope.launch { snackbarHostState.showSnackbar("Please allow notifications for reminders.") }
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) // Or request after showing rationale dialog
                }

                else -> {
                    Log.i("MainActivity", "Requesting POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

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

        checkAndRequestNotificationPermission()

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
        val listViewModel: TaskListViewModel = koinViewModel()
        val moreViewModel: MoreViewModel = koinViewModel() // Get MoreViewModel instance
        val context = LocalContext.current // Get context
        val appWidgetManager = AppWidgetManager.getInstance(context) // Get AppWidgetManager
        val snackbarHostState = remember { SnackbarHostState() } // For showing messages

        // Handle MoreViewModel Effects (like triggering widget add)
        LaunchedEffect(moreViewModel) {
            moreViewModel.effect.collectLatest { effect ->
                when (effect) {
                    is MoreEffect.TriggerWidgetPinRequest -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                            Log.i("MainActivity", "Requesting pin for widget: ${effect.componentName.shortClassName}")
                            appWidgetManager.requestPinAppWidget(effect.componentName, null, null)
                        } else {
                            Log.w("MainActivity", "Widget pinning not supported on this device/OS version.")
                            scope.launch {
                                snackbarHostState.showSnackbar("Add widgets via your phone's home screen editor.")
                            }
                        }
                    }
                    // Handle other MoreEffects if necessary
                    is MoreEffect.ShowSnackbar -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                    is MoreEffect.ShowCreateListDialog -> { /* Dialog shown in MoreDrawerContent */ }
                    is MoreEffect.NavigateToTasks -> { /* Navigation handled elsewhere */ }
                }
            }
        }


        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                MoreDrawerContent(
                    drawerState = drawerState,
                    viewModel = moreViewModel, // Pass the view model instance
                    onNavigateToTasks = { listId, sectionId ->
                        scope.launch {
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

                            // --- Navigation Logic (Keep as is) ---
                            val routeBase = Screen.Tasks.routeBase
                            val route = if (listId == null && sectionId == null) {
                                routeBase // Navigate to base "tasks" for "All Tasks"
                            } else {
                                // Build route with non-null parameters
                                val params = listOfNotNull(
                                    listId?.let { "listId=$it" },
                                    sectionId?.let { "sectionId=$it" }
                                ).joinToString("&")
                                "$routeBase?$params"
                            }

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
                snackbarHost = { SnackbarHost(snackbarHostState) }, // Add SnackbarHost
                bottomBar = {
                    BottomNavigationBar(
                        navController = navController,
                        onMoreClick = { scope.launch { drawerState.open() } }
                    )
                }
            ) { innerPadding ->
                MainNavigation(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                    taskListViewModel = listViewModel
                )
            }
        }
    }
}
