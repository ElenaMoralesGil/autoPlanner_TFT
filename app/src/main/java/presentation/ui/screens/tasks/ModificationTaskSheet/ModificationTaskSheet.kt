package com.elena.autoplanner.presentation.ui.screens.tasks.ModificationTaskSheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.presentation.intents.TaskEditIntent
import com.elena.autoplanner.presentation.ui.screens.tasks.RepeatAlertDialog
import com.elena.autoplanner.presentation.ui.screens.tasks.ReminderAlertDialog
import com.elena.autoplanner.presentation.ui.screens.tasks.DurationAlertDialog
import com.elena.autoplanner.presentation.ui.screens.tasks.StartEndDateAlertDialog
import com.elena.autoplanner.presentation.ui.screens.tasks.TimeDialogType
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Sheet for creating and editing tasks
 * Follows MVI architecture by observing state and dispatching intents
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModificationTaskSheet(
    taskToEdit: Task? = null,
    taskEditViewModel: TaskEditViewModel,
    onClose: () -> Unit
) {
    // Observe state
    val state by taskEditViewModel.state.collectAsState()

    // Local state for dialogs
    var openDialog by remember { mutableStateOf<TimeDialogType?>(null) }
    var showSubtasksSection by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }

    // Determine if we're in edit or create mode
    val isEditMode = state?.isNewTask == false

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Display error message if any
            state?.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Header with title and actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    taskEditViewModel.sendIntent(TaskEditIntent.Cancel)
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }

                Text(
                    text = if (isEditMode) "Edit Task" else "Add Task",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(
                    onClick = {
                        taskEditViewModel.sendIntent(TaskEditIntent.SaveTask)
                    },
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Confirm")
                }
            }

            // Task name field
            OutlinedTextField(
                value = state?.name ?: "",
                onValueChange = { taskEditViewModel.sendIntent(TaskEditIntent.UpdateName(it)) },
                label = { Text("Task name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Configuration options
            ConfigurationOptionsRow(
                onTimeClick = { openDialog = TimeDialogType.StartDate },
                onPriorityClick = { showPriorityDialog = true },
                onListsClick = { /* Show lists dialog */ },
                onSubtasksClick = { showSubtasksSection = !showSubtasksSection }
            )

            // Display task configuration if any
            if (hasAnyConfig(state)) {
                Spacer(modifier = Modifier.height(16.dp))
                state?.let {
                    TaskConfigDisplay(
                        startDate = it.startDateConf,
                        endDate = it.endDateConf,
                        duration = it.durationConf,
                        reminder = it.reminderPlan,
                        repeat = it.repeatPlan,
                        priority = it.priority
                    )
                }
            }

            // Subtasks section
            if (showSubtasksSection) {
                Spacer(modifier = Modifier.height(8.dp))
                state?.let {
                    SubtasksSection(
                        subtasks = it.subtasks,
                        onSubtaskToggled = { subtaskId, isCompleted ->
                            val subtask = it.subtasks.find { s -> s.id == subtaskId }
                            subtask?.let { s ->
                                taskEditViewModel.sendIntent(
                                    TaskEditIntent.UpdateSubtask(s.copy(isCompleted = isCompleted))
                                )
                            }
                        },
                        onSubtaskAdded = { subtaskName ->
                            taskEditViewModel.sendIntent(TaskEditIntent.AddSubtask(subtaskName))
                        },
                        onSubtaskDeleted = { subtask ->
                            taskEditViewModel.sendIntent(TaskEditIntent.RemoveSubtask(subtask.id))
                        },
                        showDeleteButton = true,
                        showAddButton = true,
                        errorMessage = it.error
                    )
                }
            }
        }
    }

    // Display various configuration dialogs
    when (openDialog) {
        TimeDialogType.StartDate -> {
            StartEndDateAlertDialog(
                label = "Start date",
                existing = state?.startDateConf,
                highlightDate = null,
                onDismiss = { openDialog = null },
                onReady = { newVal ->
                    taskEditViewModel.sendIntent(TaskEditIntent.UpdateStartDateConf(newVal))
                    openDialog = null
                }
            )
        }

        TimeDialogType.EndDate -> {
            StartEndDateAlertDialog(
                label = "End date",
                existing = state?.endDateConf,
                highlightDate = state?.startDateConf?.dateTime?.toLocalDate(),
                onDismiss = { openDialog = null },
                validator = { planning ->
                    planning?.let {
                        if (state?.startDateConf != null) {
                            if (it.dateTime?.isBefore(state?.startDateConf!!.dateTime) == true)
                                "End date cannot be before start date."
                            else null
                        } else {
                            if (it.dateTime?.toLocalDate()?.isBefore(LocalDate.now()) == true)
                                "End date cannot be before today."
                            else null
                        }
                    }
                },
                onReady = { newVal ->
                    taskEditViewModel.sendIntent(TaskEditIntent.UpdateEndDateConf(newVal))
                    openDialog = null
                }
            )
        }

        TimeDialogType.Duration -> {
            DurationAlertDialog(
                existing = state?.durationConf,
                onDismiss = { openDialog = null },
                onReady = { newVal ->
                    taskEditViewModel.sendIntent(TaskEditIntent.UpdateDuration(newVal))
                    openDialog = null
                }
            )
        }

        TimeDialogType.Reminder -> {
            ReminderAlertDialog(
                existing = state?.reminderPlan,
                onDismiss = { openDialog = null },
                onReady = { newVal ->
                    taskEditViewModel.sendIntent(TaskEditIntent.UpdateReminder(newVal))
                    openDialog = null
                }
            )
        }

        TimeDialogType.Repeat -> {
            RepeatAlertDialog(
                existing = state?.repeatPlan,
                onDismiss = { openDialog = null },
                onReady = { newVal ->
                    taskEditViewModel.sendIntent(TaskEditIntent.UpdateRepeat(newVal))
                    openDialog = null
                }
            )
        }

        else -> {}
    }

    // Display priority dialog if needed
    if (showPriorityDialog) {
        PriorityDialog(
            currentPriority = state?.priority ?: Priority.NONE,
            onDismiss = { showPriorityDialog = false },
            onSelectPriority = { newPriority ->
                taskEditViewModel.sendIntent(TaskEditIntent.UpdatePriority(newPriority))
                showPriorityDialog = false
            }
        )
    }
}

/**
 * Check if task has any configuration
 */
private fun hasAnyConfig(state: com.elena.autoplanner.presentation.states.TaskEditState?): Boolean {
    if (state == null) return false

    return state.startDateConf != null ||
            state.endDateConf != null ||
            state.durationConf != null ||
            state.reminderPlan != null ||
            state.repeatPlan != null ||
            state.priority != Priority.NONE
}