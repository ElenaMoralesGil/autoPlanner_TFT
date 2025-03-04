package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.presentation.utils.DateTimeFormatters
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatDurationForDisplay
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatReminderForDisplay
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatRepeatForDisplay
import com.elena.autoplanner.presentation.utils.NewTaskData
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskSheet(
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

            ActionButtonsRow(
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
                SubtaskSection(
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
        PrioritySelectDialog(
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

@Composable
private fun ActionButtonsRow(
    onTimeClick: () -> Unit,
    onPriorityClick: () -> Unit,
    onListsClick: () -> Unit,
    onSubtasksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(
            icon = painterResource(R.drawable.ic_calendar),
            label = "Time",
            onClick = onTimeClick
        )

        ActionButton(
            icon = painterResource(R.drawable.priority),
            label = "Priority",
            onClick = onPriorityClick
        )

        ActionButton(
            icon = painterResource(R.drawable.ic_lists),
            label = "Lists",
            onClick = onListsClick
        )

        ActionButton(
            icon = painterResource(R.drawable.ic_subtasks),
            label = "Subtasks",
            onClick = onSubtasksClick
        )
    }
}

@Composable
private fun ActionButton(
    icon: Painter,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            modifier = Modifier
                .size(24.dp)
                .padding(4.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
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

@Composable
fun TaskConfigDisplay(
    startDate: TimePlanning?,
    endDate: TimePlanning?,
    duration: DurationPlan?,
    reminder: ReminderPlan?,
    repeat: RepeatPlan?,
    priority: Priority
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            startDate?.let {
                ConfigItemWithPainter(
                    painter = painterResource(R.drawable.ic_calendar),
                    label = "Starts",
                    value = DateTimeFormatters.formatDateTimeWithPeriod(it)
                )
            }

            endDate?.let {
                ConfigItemWithPainter(
                    painter = painterResource(R.drawable.ic_calendar),
                    label = "Ends",
                    value = DateTimeFormatters.formatDateTimeWithPeriod(it)
                )
            }

            duration?.let {
                ConfigItemWithPainter(
                    painter = painterResource(R.drawable.ic_duration),
                    label = "Duration",
                    value = formatDurationForDisplay(it)
                )
            }

            reminder?.let {
                ConfigItemWithPainter(
                    painter = painterResource(R.drawable.ic_reminder),
                    label = "Reminder",
                    value = formatReminderForDisplay(it)
                )
            }

            repeat?.let {
                ConfigItemWithPainter(
                    painter = painterResource(R.drawable.ic_repeat),
                    label = "Repeat",
                    value = formatRepeatForDisplay(it)
                )
            }

            if (priority != Priority.NONE) {
                ConfigItemWithPainter(
                    painter = painterResource(R.drawable.priority),
                    label = "Priority",
                    value = priority.name.lowercase(Locale.ROOT)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    color = getPriorityColor(priority)
                )
            }
        }
    }
}

@Composable
private fun ConfigItemWithPainter(
    painter: Painter,
    label: String,
    value: String,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color.takeIf { it != Color.Unspecified }
                ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = color.takeIf { it != Color.Unspecified }
                    ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun getPriorityColor(priority: Priority): Color {
    return when (priority) {
        Priority.HIGH -> Color(0xFFE57373)
        Priority.MEDIUM -> Color(0xFFFFB74D)
        Priority.LOW -> Color(0xFF81C784)
        Priority.NONE -> Color.Unspecified
    }
}
@Composable
fun PrioritySelectDialog(
    currentPriority: Priority,
    onDismiss: () -> Unit,
    onSelectPriority: (Priority) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Priority") },
        text = {
            Column {
                Priority.entries.forEach { priority ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPriority(priority) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (priority == currentPriority),
                            onClick = { onSelectPriority(priority) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = priority.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}@Composable
fun SubtaskSection(
    subtasks: List<Subtask>,
    onSubtaskToggled: (subtaskId: Int, isCompleted: Boolean) -> Unit,
    onSubtaskAdded: (String) -> Unit,
    onSubtaskDeleted: (Subtask) -> Unit,
    showDeleteButton: Boolean,
    showAddButton: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {

    var newSubtaskText by remember { mutableStateOf("") }
    Column(modifier = modifier) {
        Text(
            text = "Subtasks",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }


        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .heightIn(min = 0.dp, max = 200.dp)
        ) {
            items(subtasks, key = { it.id }) { subtask ->
                SubtaskItem(
                    subtask = subtask,
                    showDeleteButton = showDeleteButton,
                    onCheckedChange = { checked ->
                        onSubtaskToggled(subtask.id, checked)
                    },
                    onDelete = {
                        onSubtaskDeleted(subtask)
                    }
                )
            }
        }



       if (showAddButton) {
           Row(
               verticalAlignment = Alignment.CenterVertically,
               modifier = Modifier.padding(horizontal = 16.dp)
           ) {
               OutlinedTextField(
                   value = newSubtaskText,
                   onValueChange = { newSubtaskText = it },
                   modifier = Modifier.weight(1f),
                   placeholder = { Text("Add subtask") }
               )
               IconButton(
                   onClick = {
                       if (newSubtaskText.isNotBlank()) {
                           onSubtaskAdded(newSubtaskText)
                           newSubtaskText = ""
                       }
                   }
               ) {
                   Icon(Icons.Default.Add, contentDescription = "Add subtask")
               }
           }
           if (newSubtaskText.isBlank()) {
               Text(
                   text = "Subtask name required",
                   color = MaterialTheme.colorScheme.error,
                   modifier = Modifier.padding(start = 16.dp)
               )
           }
       }
    }
}

@Composable
fun SubtaskItem(
    subtask: Subtask,
    showDeleteButton: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = subtask.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )

        if (showDeleteButton) {
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Delete subtask",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}