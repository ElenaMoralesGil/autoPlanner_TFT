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
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.states.TaskState
import com.elena.autoplanner.presentation.ui.screens.tasks.AddEditTaskSheet
import com.elena.autoplanner.presentation.ui.screens.tasks.TaskDetailSheet
import com.elena.autoplanner.presentation.ui.utils.ErrorMessage
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun TasksScreen(viewModel: TaskViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    var showAddEditSheet by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(Unit) {
        viewModel.sendIntent(TaskIntent.LoadTasks)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            state?.let {
                TasksTopBar(
                    state = it,
                    onStatusSelected = { status ->
                        viewModel.sendIntent(
                            TaskIntent.UpdateStatusFilter(
                                status
                            )
                        )
                    },
                    onTimeFrameSelected = { tf ->
                        viewModel.sendIntent(
                            TaskIntent.UpdateTimeFrameFilter(
                                tf
                            )
                        )
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
                    when (val uiState = currentState.uiState) {
                        TaskState.UiState.Loading -> LoadingIndicator()
                        is TaskState.UiState.Error -> uiState.message?.let { ErrorMessage(it) }
                        else -> {
                            if (currentState.isEmpty()) {
                                EmptyState()
                            } else {
                                TasksSectionContent(
                                    state = currentState,
                                    onTaskChecked = { task, checked ->
                                        viewModel.sendIntent(
                                            TaskIntent.ToggleTaskCompletion(
                                                task,
                                                checked
                                            )
                                        )
                                    },
                                    onTaskSelected = { task ->
                                        selectedTaskId = task.id
                                    },
                                    onDelete = { task ->
                                        viewModel.sendIntent(TaskIntent.DeleteTask(task.id))
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
        }
    )

    selectedTaskId?.let { taskId ->
        TaskDetailSheet(
            taskId = taskId,
            onDismiss = {
                selectedTaskId = null
            },
            viewModel = viewModel,
            onSubtaskDeleted = { subtask ->
                viewModel.sendIntent(TaskIntent.DeleteSubtask(taskId, subtask.id))
            },
            onSubtaskAdded = { subtaskName ->
                viewModel.sendIntent(TaskIntent.AddSubtask(taskId, subtaskName))
            },
            onDelete = {
                viewModel.sendIntent(TaskIntent.DeleteTask(taskId))
                selectedTaskId = null
            },
            onSubtaskToggled = { subtaskId, isCompleted ->
                viewModel.sendIntent(TaskIntent.ToggleSubtask(taskId, subtaskId, isCompleted))
            },
            onEdit = {
                val foundTask = state?.tasks?.find { it.id == taskId }
                taskToEdit = foundTask
                selectedTaskId = null
                showAddEditSheet = true
            }
        )
    }

    if (showAddEditSheet) {
        AddEditTaskSheet(
            taskToEdit = taskToEdit,
            onClose = {
                showAddEditSheet = false
                if (taskToEdit != null) {
                    selectedTaskId = taskToEdit?.id
                }
                taskToEdit = null
            },
            onCreateTask = { newTaskData ->
                viewModel.sendIntent(TaskIntent.CreateTask(newTaskData))
                showAddEditSheet = false
            },
            onUpdateTask = { updatedTask ->
                viewModel.sendIntent(TaskIntent.UpdateTask(updatedTask))
                showAddEditSheet = false
                selectedTaskId = updatedTask.id
            }
        )
    }
}