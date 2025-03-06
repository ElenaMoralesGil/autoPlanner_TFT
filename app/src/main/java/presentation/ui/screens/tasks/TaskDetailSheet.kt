package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.presentation.intents.TaskDetailIntent
import com.elena.autoplanner.presentation.ui.screens.tasks.ModificationTaskSheet.SubtasksSection
import com.elena.autoplanner.presentation.ui.screens.tasks.ModificationTaskSheet.TaskConfigDisplay
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    taskId: Int,
    onDismiss: () -> Unit,
    viewModel: TaskDetailViewModel
) {
    // Observe state
    val state by viewModel.state.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        if (state?.isLoading == true) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state?.task == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Task not found")
            }
        } else {
            val task = state?.task!!

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = task.name, style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                TaskConfigDisplay(
                    startDate = task.startDateConf,
                    endDate = task.endDateConf,
                    duration = task.durationConf,
                    reminder = task.reminderPlan,
                    repeat = task.repeatPlan,
                    priority = task.priority,
                ) // Subtasks section
                SubtasksSection(
                    subtasks = task.subtasks,
                    onSubtaskToggled = { subtaskId, isCompleted ->
                        viewModel.sendIntent(TaskDetailIntent.ToggleSubtask(subtaskId, isCompleted))
                    },
                    onSubtaskAdded = { subtaskName ->
                        viewModel.sendIntent(TaskDetailIntent.AddSubtask(subtaskName))
                    },
                    onSubtaskDeleted = { subtask ->
                        viewModel.sendIntent(TaskDetailIntent.DeleteSubtask(subtask.id))
                    },
                    showDeleteButton = true,
                    showAddButton = true,
                    errorMessage = state?.error
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { viewModel.sendIntent(TaskDetailIntent.DeleteTask) }
                    ) {
                        Text("Delete Task", color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = { viewModel.sendIntent(TaskDetailIntent.EditTask) }
                    ) {
                        Text("Edit Task")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mark as completed:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = task.isCompleted,
                        onCheckedChange = { isCompleted ->
                            viewModel.sendIntent(TaskDetailIntent.ToggleCompletion(isCompleted))
                        }
                    )
                }
            }
        }
    }
}