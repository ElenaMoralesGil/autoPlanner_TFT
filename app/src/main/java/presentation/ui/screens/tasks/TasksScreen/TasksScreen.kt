package com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.effects.TaskListEffect
import com.elena.autoplanner.presentation.intents.TaskDetailIntent
import com.elena.autoplanner.presentation.intents.TaskListIntent
import com.elena.autoplanner.presentation.ui.screens.tasks.ModificationTaskSheet.ModificationTaskSheet
import com.elena.autoplanner.presentation.ui.screens.tasks.TaskDetailSheet
import com.elena.autoplanner.presentation.ui.utils.ErrorMessage
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskListViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TasksScreen() {
    // Main ViewModel for task list operations
    val viewModel: TaskListViewModel = koinViewModel()
    // TaskViewModel is still used for operations that cross boundaries between screens
    val taskViewModel: TaskViewModel = koinViewModel()

    val state by viewModel.state.collectAsState()
    var showAddEditSheet by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    // Handle navigation effects
    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is TaskListEffect.NavigateToTaskDetail -> {
                    selectedTaskId = effect.taskId
                }

                is TaskListEffect.ShowSnackbar -> {
                    // Show snackbar message (not implemented in this snippet)
                }
            }
        }
    }

    // Initial load of tasks
    LaunchedEffect(Unit) {
        viewModel.sendIntent(TaskListIntent.LoadTasks)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            state?.let {
                TasksTopBar(
                    state = it,
                    onStatusSelected = { status ->
                        viewModel.sendIntent(TaskListIntent.UpdateStatusFilter(status))
                    },
                    onTimeFrameSelected = { timeFrame ->
                        viewModel.sendIntent(TaskListIntent.UpdateTimeFrameFilter(timeFrame))
                    }
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
                Column(modifier = Modifier.padding(innerPadding)) {
                    when {
                        currentState.isLoading -> LoadingIndicator()
                        currentState.error != null -> ErrorMessage(currentState.error)
                        currentState.filteredTasks.isEmpty() -> EmptyState()
                        else -> {
                            TasksSectionContent(
                                state = currentState,
                                onTaskChecked = { task, checked ->
                                    viewModel.sendIntent(
                                        TaskListIntent.ToggleTaskCompletion(task.id, checked)
                                    )
                                },
                                onTaskSelected = { task ->
                                    viewModel.sendIntent(TaskListIntent.SelectTask(task.id))
                                },
                                onDelete = { task ->
                                    taskViewModel.deleteTask(task.id)
                                    // Refresh task list after deletion
                                    viewModel.sendIntent(TaskListIntent.LoadTasks)
                                },
                                onEdit = { task ->
                                    taskToEdit = task
                                    showAddEditSheet = true
                                }
                            )
                        }
                    }
                }
            }
        }
    )

    // Task detail bottom sheet
    selectedTaskId?.let { taskId ->
        val taskDetailViewModel =
            getViewModel<TaskDetailViewModel>(parameters = { parametersOf(taskId) })

        TaskDetailSheet(
            taskId = taskId,
            viewModel = taskDetailViewModel,
            onDismiss = {
                selectedTaskId = null
            },
            onSubtaskDeleted = { subtask ->
                taskDetailViewModel.sendIntent(
                    TaskDetailIntent.DeleteSubtask(subtask.id)
                )
            },
            onSubtaskAdded = { subtaskName ->
                taskDetailViewModel.sendIntent(
                    TaskDetailIntent.AddSubtask(subtaskName)
                )
            },
            onDelete = {
                taskDetailViewModel.sendIntent(TaskDetailIntent.DeleteTask)
            },
            onSubtaskToggled = { subtaskId, isCompleted ->
                taskDetailViewModel.sendIntent(
                    TaskDetailIntent.ToggleSubtask(subtaskId, isCompleted)
                )
            },
            onEdit = {
                taskDetailViewModel.sendIntent(TaskDetailIntent.EditTask)
            }
        )
    }

    // Task add/edit bottom sheet
    if (showAddEditSheet) {
        val taskEditViewModel = if (taskToEdit != null) {
            getViewModel<TaskEditViewModel>(parameters = { parametersOf(taskToEdit?.id ?: 0) })
        } else {
            getViewModel<TaskEditViewModel>(parameters = { parametersOf(0) })
        }

        ModificationTaskSheet(
            taskToEdit = taskToEdit,
            onClose = {
                showAddEditSheet = false
                if (taskToEdit != null) {
                    selectedTaskId = taskToEdit?.id
                }
                taskToEdit = null
            },
            onCreateTask = { newTaskData ->
                taskViewModel.createTask(newTaskData)
                showAddEditSheet = false
                // Refresh task list after creation
                viewModel.sendIntent(TaskListIntent.LoadTasks)
            },
            onUpdateTask = { updatedTask ->
                taskViewModel.updateTask(updatedTask)
                showAddEditSheet = false
                selectedTaskId = updatedTask.id
                // Refresh task list after update
                viewModel.sendIntent(TaskListIntent.LoadTasks)
            }
        )
    }
}