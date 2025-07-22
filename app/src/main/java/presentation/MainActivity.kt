package com.elena.autoplanner.presentation

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.elena.autoplanner.AutoPlannerApplication
import com.elena.autoplanner.R
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    private val dataSeeder: DataSeeder by inject()
    private val taskRepository: TaskRepository by inject()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {

                    Log.d("MainActivity", "Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showNotificationPermissionRationale()
                }
                else -> {

                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showNotificationPermissionRationale() {

        AlertDialog.Builder(this)
            .setTitle("Notification Permission Needed")
            .setMessage("Notifications are required to remind you about your tasks.")
            .setPositiveButton("Grant") { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            MainApp { navController ->
                handleNotificationNavigation(intent, navController)
            }
        }
    }

    private fun handleNotificationNavigation(intent: Intent?, navController: NavHostController) {
        intent?.let {
            val taskId = it.getIntExtra("navigate_to_task_id", -1)
            if (taskId != -1) {
                Log.d("MainActivity", "Navigating to task $taskId from notification")

                lifecycleScope.launch {
                    delay(100) 

                    try {

                        val taskResult = taskRepository.getTask(taskId)
                        if (taskResult is TaskResult.Success) {
                            val task = taskResult.data

                            val route = when {
                                task.listId != null && task.sectionId != null -> {
                                    "tasks?listId=${task.listId}&sectionId=${task.sectionId}"
                                }

                                task.listId != null -> {
                                    "tasks?listId=${task.listId}"
                                }

                                else -> {
                                    "tasks" 
                                }
                            }

                            Log.d(
                                "MainActivity",
                                "Navigating to route: $route for task in list: ${task.listName}"
                            )

                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }

                        } else {
                            Log.e("MainActivity", "Failed to fetch task details for navigation")

                            navController.navigate("tasks")
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error handling notification navigation", e)
                    }
                }

                it.removeExtra("navigate_to_task_id")
            }
        }
    }
}

@Composable
fun MainApp(onNavControllerReady: (NavHostController) -> Unit = {}) {
    AppTheme {
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val listViewModel: TaskListViewModel = koinViewModel()
        val moreViewModel: MoreViewModel = koinViewModel()
        val context = LocalContext.current
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(navController) {
            onNavControllerReady(navController)
        }

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

                    is MoreEffect.ShowSnackbar -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                    is MoreEffect.ShowCreateListDialog -> {}
                    is MoreEffect.NavigateToTasks -> {}
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                MoreDrawerContent(
                    drawerState = drawerState,
                    viewModel = moreViewModel, 
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

                            val routeBase = Screen.Tasks.routeBase
                            val route = if (listId == null && sectionId == null) {
                                routeBase 
                            } else {

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
                snackbarHost = { SnackbarHost(snackbarHostState) }, 
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