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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.presentation.ui.screens.tasks.TimeConfigSheet
import com.elena.autoplanner.presentation.utils.NewTaskData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModificationTaskSheet(
    taskToEdit: Task? = null,
    onClose: () -> Unit,
    onCreateTask: (NewTaskData) -> Unit,
    onUpdateTask: (Task) -> Unit,
    errorMessage: String? = null
) {
    val isEditMode = taskToEdit != null

    var taskName by remember { mutableStateOf(taskToEdit?.name ?: "") }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: Priority.NONE) }
    var startDateConf by remember { mutableStateOf(taskToEdit?.startDateConf) }
    var endDateConf by remember { mutableStateOf(taskToEdit?.endDateConf) }
    var durationConf by remember { mutableStateOf(taskToEdit?.durationConf) }
    var reminderPlan by remember { mutableStateOf(taskToEdit?.reminderPlan) }
    var repeatPlan by remember { mutableStateOf(taskToEdit?.repeatPlan) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    var subtasks by remember { mutableStateOf(taskToEdit?.subtasks ?: emptyList()) }
    var showSubtasksSection by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showTimeConfigSheet by remember { mutableStateOf(false) }

    LaunchedEffect(taskToEdit) {
        taskName = taskToEdit?.name ?: ""
        priority = taskToEdit?.priority ?: Priority.NONE
        startDateConf = taskToEdit?.startDateConf
        endDateConf = taskToEdit?.endDateConf
        durationConf = taskToEdit?.durationConf
        reminderPlan = taskToEdit?.reminderPlan
        repeatPlan = taskToEdit?.repeatPlan
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            (errorMessage ?: localErrorMessage)?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
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
                        if (taskName.isBlank()) {
                            localErrorMessage = "Task name cannot be empty"
                        } else {
                            if (isEditMode) {
                                onUpdateTask(
                                    taskToEdit!!.copy(
                                        name = taskName,
                                        priority = priority,
                                        startDateConf = startDateConf,
                                        endDateConf = endDateConf,
                                        durationConf = durationConf,
                                        reminderPlan = reminderPlan,
                                        repeatPlan = repeatPlan,
                                        subtasks = subtasks
                                    )
                                )
                            } else {
                                onCreateTask(
                                    NewTaskData(
                                        name = taskName,
                                        priority = priority,
                                        startDateConf = startDateConf,
                                        endDateConf = endDateConf,
                                        durationConf = durationConf,
                                        reminderPlan = reminderPlan,
                                        repeatPlan = repeatPlan,
                                        subtasks = subtasks
                                    )
                                )
                            }
                            localErrorMessage = null
                        }
                    },
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Confirm")
                }
            }

            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text("Task name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConfigurationOptionsRow(
                onTimeClick = { showTimeConfigSheet = true },
                onPriorityClick = { showPriorityDialog = true },
                onListsClick = { },
                onSubtasksClick = {
                    showSubtasksSection = !showSubtasksSection
                }
            )

            if (hasAnyConfig(startDateConf, endDateConf, durationConf, reminderPlan, repeatPlan, priority)) {
                Spacer(modifier = Modifier.height(16.dp))
                TaskConfigDisplay(
                    startDate = startDateConf,
                    endDate = endDateConf,
                    duration = durationConf,
                    reminder = reminderPlan,
                    repeat = repeatPlan,
                    priority = priority
                )
            }

            if (showSubtasksSection) {
                Spacer(modifier = Modifier.height(8.dp))
                SubtasksSection(
                    subtasks = subtasks,
                    onSubtaskToggled = { subtaskId, isCompleted ->
                        subtasks = subtasks.map {
                            if (it.id == subtaskId) it.copy(isCompleted = isCompleted)
                            else it
                        }
                    },
                    onSubtaskAdded = { subtaskName ->
                        val nextId = if (subtasks.isEmpty()) 1 else subtasks.maxOf { it.id } + 1
                        val newSubtask = Subtask(
                            id = nextId,
                            name = subtaskName,
                            isCompleted = false
                        )
                        subtasks = subtasks + newSubtask
                    },
                    onSubtaskDeleted = { subtask ->
                        subtasks = subtasks.filter { it.id != subtask.id }
                    },
                    showDeleteButton = true,
                    showAddButton = true,
                    errorMessage = null
                )
            }
        }
    }

    if (showPriorityDialog) {
        PriorityDialog(
            currentPriority = priority,
            onDismiss = { showPriorityDialog = false },
            onSelectPriority = { newPriority ->
                priority = newPriority
                showPriorityDialog = false
            }
        )
    }

    if (showTimeConfigSheet) {
        TimeConfigSheet(
            onClose = { showTimeConfigSheet = false },
            currentStart = startDateConf,
            currentEnd = endDateConf,
            currentDuration = durationConf,
            currentReminder = reminderPlan,
            currentRepeat = repeatPlan,
            onSaveAll = { newStart, newEnd, newDur, newRem, newRep ->
                startDateConf = newStart
                endDateConf = newEnd
                durationConf = newDur
                reminderPlan = newRem
                repeatPlan = newRep
            }
        )
    }
}
private fun hasAnyConfig(
    start: TimePlanning?,
    end: TimePlanning?,
    duration: DurationPlan?,
    reminder: ReminderPlan?,
    repeat: RepeatPlan?,
    priority: Priority
): Boolean {
    return start != null || end != null || duration != null ||
            reminder != null || repeat != null || priority != Priority.NONE
}