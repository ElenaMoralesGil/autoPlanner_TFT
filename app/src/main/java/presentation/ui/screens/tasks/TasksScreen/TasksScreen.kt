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
import androidx.navigation.NavHostController
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.effects.TaskDetailEffect
import com.elena.autoplanner.presentation.effects.TaskEditEffect
import com.elena.autoplanner.presentation.effects.TaskListEffect
import com.elena.autoplanner.presentation.intents.TaskDetailIntent
import com.elena.autoplanner.presentation.intents.TaskEditIntent
import com.elena.autoplanner.presentation.intents.TaskListIntent
import com.elena.autoplanner.presentation.ui.screens.tasks.TaskDetailSheet
import com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet.ModificationTaskSheet
import com.elena.autoplanner.presentation.ui.screens.more.CreateEditListDialog
import com.elena.autoplanner.presentation.ui.utils.ErrorMessage
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator
import com.elena.autoplanner.presentation.ui.utils.RepeatTaskDeleteDialog
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
    navController: NavHostController,
) {

    val state by listViewModel.state.collectAsState()
    var showAddEditSheet by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var selectedInstanceIdentifier by remember { mutableStateOf<String?>(null) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showRepeatDeleteDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showEditListDialog by remember { mutableStateOf(false) }
    var showEditSectionsDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(listViewModel) {
        listViewModel.effect.collectLatest { effect ->
            when (effect) {
                is TaskListEffect.NavigateToTaskDetail -> {
                    selectedTaskId = effect.taskId
                    selectedInstanceIdentifier = effect.instanceIdentifier
                }

                is TaskListEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is TaskListEffect.ShowRepeatTaskDeleteDialog -> {
                    taskToDelete = effect.task
                    showRepeatDeleteDialog = true
                }

                is TaskListEffect.ShowEditListDialog -> {
                    showEditListDialog = true
                }

                is TaskListEffect.ShowEditSectionsDialog -> {
                    showEditSectionsDialog = true
                }
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
                                    // Manejar instancias repetidas especialmente
                                    val taskIdToSelect =
                                        if (task.isRepeatedInstance && task.parentTaskId != null) {
                                            task.parentTaskId
                                        } else {
                                            task.id
                                        }

                                    listViewModel.sendIntent(
                                        TaskListIntent.SelectTask(
                                            taskIdToSelect,
                                            task.instanceIdentifier
                                        )
                                    )
                                },
                                onDelete = { task ->
                                    listViewModel.sendIntent(
                                        TaskListIntent.DeleteRepeatableTask(
                                            task.copy(instanceIdentifier = task.instanceIdentifier)
                                        )
                                    )
                                },
                                onEdit = { task ->
                                    taskToEdit = task
                                    showAddEditSheet = true
                                },
                                listViewModel = listViewModel
                            )
                        }
                    }
                }
            } ?: Column(modifier = Modifier.padding(innerPadding)) { LoadingIndicator() }
        }
    )

    selectedTaskId?.let { taskId ->

        val detailViewModel: TaskDetailViewModel =
            koinViewModel(parameters = { parametersOf(taskId, selectedInstanceIdentifier) })

        LaunchedEffect(detailViewModel) {
            detailViewModel.effect.collectLatest { effect ->
                when (effect) {
                    is TaskDetailEffect.NavigateBack -> {
                        selectedTaskId = null

                    }

                    is TaskDetailEffect.NavigateToEdit -> {

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

        LaunchedEffect(taskId) {
            detailViewModel.sendIntent(TaskDetailIntent.LoadTask(taskId))
        }

        TaskDetailSheet(
            taskId = taskId,
            onDismiss = { selectedTaskId = null },
            viewModel = detailViewModel
        )
    }

    if (showAddEditSheet) {
        val editViewModel: TaskEditViewModel =
            koinViewModel(parameters = { parametersOf(taskToEdit?.id ?: 0) })

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

        LaunchedEffect(taskToEdit?.id) {
            editViewModel.sendIntent(TaskEditIntent.LoadTask(taskToEdit?.id ?: 0))
        }

        ModificationTaskSheet(
            taskEditViewModel = editViewModel,
            onClose = {
                editViewModel.sendIntent(TaskEditIntent.Cancel)
            }
        )
    }

    if (showRepeatDeleteDialog && taskToDelete != null) {
        RepeatTaskDeleteDialog(
            task = taskToDelete!!,
            onOptionSelected = { option ->
                listViewModel.sendIntent(
                    TaskListIntent.ConfirmRepeatableTaskDeletion(
                        taskToDelete!!,
                        option
                    )
                )
                showRepeatDeleteDialog = false
                taskToDelete = null
            },
            onDismiss = {
                showRepeatDeleteDialog = false
                taskToDelete = null
            }
        )
    }

    if (showEditListDialog) {
        // Aquí debes implementar el diálogo para editar la lista
        // Puedes crear un nuevo Composable para el diálogo y llamarlo aquí
    }

    if (showEditSectionsDialog) {
        // Aquí debes implementar el diálogo para editar las secciones
        // Puedes crear un nuevo Composable para el diálogo y llamarlo aquí
    }
}