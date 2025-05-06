package com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.effects.TaskDetailEffect
import com.elena.autoplanner.presentation.effects.TaskEditEffect
import com.elena.autoplanner.presentation.effects.TaskListEffect
import com.elena.autoplanner.presentation.intents.TaskDetailIntent
import com.elena.autoplanner.presentation.intents.TaskEditIntent
import com.elena.autoplanner.presentation.intents.TaskListIntent
import com.elena.autoplanner.presentation.ui.screens.tasks.TaskDetailSheet
import com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet.ModificationTaskSheet
import com.elena.autoplanner.presentation.ui.utils.ErrorMessage
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskListViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TasksScreen(
    onNavigateToPlanner: () -> Unit,
    listViewModel: TaskListViewModel,
    navController: androidx.navigation.NavHostController,
) {

    val state by listViewModel.state.collectAsState()
    var showAddEditSheet by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation effects from TaskListViewModel

    LaunchedEffect(listViewModel) {
        listViewModel.effect.collectLatest { effect ->
            when (effect) {
                is TaskListEffect.NavigateToTaskDetail -> {
                    selectedTaskId = effect.taskId
                }

                is TaskListEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is TaskListEffect.ShowEditListDialog -> TODO()
                is TaskListEffect.ShowEditSectionsDialog -> TODO()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            state?.let {
                TasksTopBar(
                    state = it,
                    currentListName = it.currentListName,
                    onStatusSelected = { status ->
                        listViewModel.sendIntent(TaskListIntent.UpdateStatusFilter(status))
                    },
                    onTimeFrameSelected = { timeFrame ->
                        listViewModel.sendIntent(TaskListIntent.UpdateTimeFrameFilter(timeFrame))
                    },
                    onPlannerClick = onNavigateToPlanner,
                    onShowAllTasks = { listViewModel.sendIntent(TaskListIntent.ViewAllTasks) },
                    onEditList = { listViewModel.sendIntent(TaskListIntent.RequestEditList) },
                    onEditSections = { listViewModel.sendIntent(TaskListIntent.RequestEditSections) }

                )
            }
        },
        floatingActionButton = {
            AddTaskButton {
                taskToEdit = null
                showAddEditSheet = true
            }
        },
        content = { innerPadding ->
            state?.let { currentState ->
                Log.d("TasksScreen", "Rendering UI with State: requested(L=${currentState.requestedListId}, S=${currentState.requestedSectionId}), current(L=${currentState.currentListId}, S=${currentState.currentSectionId}), isLoading=${currentState.isLoading}, isNavigating=${currentState.isNavigating}, tasks=${currentState.filteredTasks.size}")
                val isLoadingOrNavigating = currentState.isLoading || currentState.isNavigating

                Log.d("TasksScreen", "Calculated shouldShowLoading = $isLoadingOrNavigating")

                Column(modifier = Modifier.padding(innerPadding)) {
                    when {

                        isLoadingOrNavigating -> {
                            Log.d("TasksScreen", "--> Rendering LoadingIndicator (isLoadingOrNavigating=true)")
                            LoadingIndicator()
                        }
                        currentState.error != null -> {
                            Log.d("TasksScreen", "--> Rendering ErrorMessage")
                            ErrorMessage(currentState.error)
                        }
                        currentState.tasks.isEmpty()-> {
                            Log.d("TasksScreen", "--> Rendering EmptyState (tasks empty, not loading)")
                            EmptyState()
                        }
                        else -> {
                            Log.d("TasksScreen", "--> Rendering TasksSectionContent (tasks: ${currentState.tasks.size}, filtered: ${currentState.filteredTasks.size})")
                            TasksSectionContent(
                                state = currentState,
                                onTaskChecked = { task, checked ->
                                    listViewModel.sendIntent(
                                        TaskListIntent.ToggleTaskCompletion(task.id, checked)
                                    )
                                },
                                onTaskSelected = { task ->
                                    listViewModel.sendIntent(TaskListIntent.SelectTask(task.id))
                                },
                                onDelete = { task ->
                                    listViewModel.sendIntent(TaskListIntent.DeleteTask(task.id))
                                },
                                onEdit = { task ->
                                    taskToEdit = task
                                    showAddEditSheet = true
                                }
                            )
                        }
                    }
                }
            } ?: Column(modifier = Modifier.padding(innerPadding)) { LoadingIndicator() }
        }
    )

    // Task detail bottom sheet
    selectedTaskId?.let { taskId ->
        // Use Koin's viewModel function with parameters for the detail view
        val detailViewModel: TaskDetailViewModel =
            koinViewModel(parameters = { parametersOf(taskId) })

        // Handle effects from TaskDetailViewModel
        LaunchedEffect(detailViewModel) {
            detailViewModel.effect.collectLatest { effect ->
                when (effect) {
                    is TaskDetailEffect.NavigateBack -> {
                        selectedTaskId = null

                    }

                    is TaskDetailEffect.NavigateToEdit -> {
                        // Load task to edit and show edit sheet
                        taskToEdit = state?.tasks?.find { it.id == effect.taskId }
                        selectedTaskId = null
                        showAddEditSheet = true
                    }

                    is TaskDetailEffect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }

        // Launch initial load task intent
        LaunchedEffect(taskId) {
            detailViewModel.sendIntent(TaskDetailIntent.LoadTask(taskId))
        }

        // Show the detail sheet
        TaskDetailSheet(
            taskId = taskId,
            onDismiss = { selectedTaskId = null },
            viewModel = detailViewModel
        )
    }

    // Task add/edit bottom sheet
    if (showAddEditSheet) {
        // Get edit ViewModel with correct parameters
        val editViewModel: TaskEditViewModel =
            koinViewModel(parameters = { parametersOf(taskToEdit?.id ?: 0) })

        // Handle effects from TaskEditViewModel
        LaunchedEffect(editViewModel) {
            editViewModel.effect.collectLatest { effect ->
                when (effect) {
                    is TaskEditEffect.NavigateBack -> {
                        showAddEditSheet = false
                        if (taskToEdit != null) {
                            selectedTaskId = taskToEdit?.id
                        }
                        taskToEdit = null
                    }

                    is TaskEditEffect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }

        // Launch initial load task intent if in edit mode
        LaunchedEffect(taskToEdit?.id) {
            editViewModel.sendIntent(TaskEditIntent.LoadTask(taskToEdit?.id ?: 0))
        }

        // Show the edit sheet
        ModificationTaskSheet(
            taskEditViewModel = editViewModel,
            onClose = {
                editViewModel.sendIntent(TaskEditIntent.Cancel)
            }
        )
    }
}