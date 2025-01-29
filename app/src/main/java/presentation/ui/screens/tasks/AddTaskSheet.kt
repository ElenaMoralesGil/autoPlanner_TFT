package com.elena.autoplanner.presentation.ui.screens.tasks


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.elena.autoplanner.R
import com.elena.autoplanner.presentation.utils.DateTimeFormatters
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatDurationForDisplay
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatReminderForDisplay
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatRepeatForDisplay
import com.elena.autoplanner.presentation.utils.NewTaskData
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onClose: () -> Unit,
    onAccept: (NewTaskData) -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var showTimeConfigSheet by remember { mutableStateOf(false) }
    var showPrioritySheet by remember { mutableStateOf(false) }
    var showListsSheet by remember { mutableStateOf(false) }
    var showSubtasksSheet by remember { mutableStateOf(false) }

    // Domain states
    var priority by remember { mutableStateOf(Priority.NONE) }
    var timePlanningStart by remember { mutableStateOf<TimePlanning?>(null) }
    var timePlanningEnd by remember { mutableStateOf<TimePlanning?>(null) }
    var duration by remember { mutableStateOf<DurationPlan?>(null) }
    var reminder by remember { mutableStateOf<ReminderPlan?>(null) }
    var repeat by remember { mutableStateOf<RepeatPlan?>(null) }

    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Header
            SheetHeader(
                onClose = onClose,
                onAccept = {
                    onAccept(
                        NewTaskData(
                            name = taskName,
                            priority = priority,
                            startDateConf = timePlanningStart,
                            endDateConf = timePlanningEnd,
                            durationConf = duration,
                            reminderPlan = reminder,
                            repeatPlan = repeat
                        )
                    )
                }
            )

            // Task name input
            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text("Task name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            ActionButtonsRow(
                onTimeClick = { showTimeConfigSheet = true },
                onPriorityClick = { showPrioritySheet = true },
                onListsClick = { showListsSheet = true },
                onSubtasksClick = { showSubtasksSheet = true }
            )

            // Show configurations if any exist
            if (hasAnyConfig(timePlanningStart, timePlanningEnd, duration, reminder, repeat, priority)) {
                Spacer(modifier = Modifier.height(16.dp))
                TaskConfigDisplay(
                    startDate = timePlanningStart,
                    endDate = timePlanningEnd,
                    duration = duration,
                    reminder = reminder,
                    repeat = repeat,
                    priority = priority
                )
            }
        }

        // Sheets
        if (showTimeConfigSheet) {
            TimeConfigSheet(
                onClose = { showTimeConfigSheet = false },
                currentStart = timePlanningStart,
                currentEnd = timePlanningEnd,
                currentDuration = duration,
                currentReminder = reminder,
                currentRepeat = repeat
            ) { newStart, newEnd, newDur, newRem, newRep ->
                timePlanningStart = newStart
                timePlanningEnd = newEnd
                duration = newDur
                reminder = newRem
                repeat = newRep
            }
        }
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
@Composable
private fun SheetHeader(
    onClose: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close"
            )
        }

        Text(
            text = "Add Task",
            style = MaterialTheme.typography.titleLarge
        )

        IconButton(
            onClick = onAccept,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Save task"
            )
        }
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
        Priority.HIGH -> Color(0xFFE57373)    // Light Red
        Priority.MEDIUM -> Color(0xFFFFB74D)  // Light Orange
        Priority.LOW -> Color(0xFF81C784)     // Light Green
        Priority.NONE -> Color.Unspecified
    }
}
@Composable
fun PriorityIndicator(priority: Priority) {
    val (color, icon) = when (priority) {
        Priority.HIGH -> Pair(Color(0xFFE57373), painterResource(R.drawable.priority))
        Priority.MEDIUM -> Pair(Color(0xFFFFB74D) , painterResource(R.drawable.priority))
        Priority.LOW -> Pair(Color(0xFF81C784), painterResource(R.drawable.priority))
        Priority.NONE -> Pair(Color.Transparent, null)
    }

    icon?.let {
        Icon(
            painter = it,
            contentDescription = "Priority",
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}
@Composable
fun SubtaskProgress(subtasks: List<Subtask>) {
    val completedCount = subtasks.count { it.isCompleted }
    val progress = subtasks.size.toFloat() / completedCount

    Column {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "$completedCount/${subtasks.size} completed",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}