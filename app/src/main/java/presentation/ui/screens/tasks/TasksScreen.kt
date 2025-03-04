package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.states.TaskState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.ui.utils.ErrorMessage
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator
import com.elena.autoplanner.presentation.utils.DateTimeFormatters
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt


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
                TasksAppBar(
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
            AddTaskFAB {
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksAppBar(
    state: TaskState,
    onStatusSelected: (TaskStatus) -> Unit,
    onTimeFrameSelected: (TimeFrame) -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            Column {
                Text("Tasks", style = MaterialTheme.typography.headlineSmall)
                Text(
                    buildFilterText(state),
                    Modifier.padding(start = 2.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                )
            }
        },
        actions = {
            StatusFilterDropdown(state.filters.status, onStatusSelected)
            TimeFrameFilterDropdown(state.filters.timeFrame, onTimeFrameSelected)
        }
    )
}


@Composable
private fun AddTaskFAB(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(Icons.Default.Add, "Add Task")
    }
}

@Composable
private fun buildFilterText(state: TaskState): String {
    val filters = listOfNotNull(
        state.filters.timeFrame.takeIf { it != TimeFrame.ALL }?.displayName,
        state.filters.status.takeIf { it != TaskStatus.ALL }?.displayName
    )
    return filters.joinToString(" • ").ifEmpty { "All Tasks" }
}

@Composable
private fun StatusFilterDropdown(
    currentStatus: TaskStatus,
    onSelected: (TaskStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_filter),
                contentDescription = "Status filter",
                tint = if (currentStatus != TaskStatus.ALL) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TaskStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.displayName) },
                    onClick = {
                        onSelected(status)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(status.iconRes()),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TimeFrameFilterDropdown(
    currentTimeFrame: TimeFrame,
    onSelected: (TimeFrame) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = "Time filter",
                tint = if (currentTimeFrame != TimeFrame.ALL) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimeFrame.entries.forEach { timeFrame ->
                DropdownMenuItem(
                    text = { Text(timeFrame.displayName) },
                    onClick = {
                        onSelected(timeFrame)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(timeFrame.iconRes()),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

private fun TaskStatus.iconRes() = when (this) {
    TaskStatus.COMPLETED -> R.drawable.ic_completed
    TaskStatus.UNCOMPLETED -> R.drawable.ic_uncompleted
    else -> R.drawable.ic_lists
}

private fun TimeFrame.iconRes() = when (this) {
    TimeFrame.TODAY, TimeFrame.WEEK, TimeFrame.MONTH -> R.drawable.ic_lists
    else -> R.drawable.ic_calendar
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lists),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No Tasks Found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tap the + button to create your first task",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TasksSectionContent(
    state: TaskState,
    onTaskChecked: (Task, Boolean) -> Unit,
    onTaskSelected: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit
) {
    val tasks = state.filteredTasks
    val notDoneTasks = tasks.filter { !it.isCompleted && !it.isExpired() }
    val expiredNotCompletedTasks = tasks.filter { it.isExpired() && !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }

    val showNotDone = when (state.filters.status) {
        TaskStatus.ALL, TaskStatus.UNCOMPLETED -> true
        else -> false
    }

    val showCompleted = when (state.filters.status) {
        TaskStatus.ALL, TaskStatus.COMPLETED -> true
        else -> false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        if (showNotDone && expiredNotCompletedTasks.isNotEmpty()) {
            stickyHeader {
                EnhancedSectionHeader(
                    title = "Expired",
                    count = expiredNotCompletedTasks.size,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(expiredNotCompletedTasks, key = { it.id }) { task ->
                EnhancedTaskCard(
                    task = task,
                    onCheckedChange = { checked -> onTaskChecked(task, checked) },
                    onDelete = { onDelete(task) },
                    onEdit = { onEdit(task) },
                    onTaskSelected = { onTaskSelected(task) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (showNotDone && notDoneTasks.isNotEmpty()) {
            stickyHeader {
                EnhancedSectionHeader(
                    title = "Not Done",
                    count = notDoneTasks.size,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(notDoneTasks, key = { it.id }) { task ->
                EnhancedTaskCard(
                    task = task,
                    onCheckedChange = { checked -> onTaskChecked(task, checked) },
                    onDelete = { onDelete(task) },
                    onEdit = { onEdit(task) },
                    onTaskSelected = { onTaskSelected(task) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (showCompleted && completedTasks.isNotEmpty()) {
            stickyHeader {
                EnhancedSectionHeader(
                    title = "Completed",
                    count = completedTasks.size,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(completedTasks) { task ->
                EnhancedTaskCard(
                    task = task,
                    onCheckedChange = { checked -> onTaskChecked(task, checked) },
                    onDelete = { onDelete(task) },
                    onEdit = { onEdit(task) },
                    onTaskSelected = { onTaskSelected(task) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DeleteAction(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp)
        )
    }
}

@Composable
private fun EditAction(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = "Edit",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp)
        )
    }
}

@Composable
fun EnhancedTaskCard(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onTaskSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxOffset = with(LocalDensity.current) { 150.dp.toPx() }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 200),
        label = "cardSwipe"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (animatedOffset > 0) {
                EditAction(modifier = Modifier.weight(0.2f))
            }
            if (animatedOffset < 0) {
                DeleteAction(modifier = Modifier.weight(0.2f))
            }
        }

        Card(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-maxOffset, maxOffset)
                        },
                        onDragEnd = {
                            when {
                                offsetX > maxOffset * 0.5f -> {
                                    onEdit()
                                    offsetX = 0f
                                }

                                offsetX < -maxOffset * 0.5f -> {
                                    onDelete()
                                    offsetX = 0f
                                }

                                else -> offsetX = 0f
                            }
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = when (task.priority) {
                                Priority.HIGH -> MaterialTheme.colorScheme.error
                                Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                                Priority.LOW -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (offsetX == 0f) onTaskSelected()
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.startDateConf != null || task.durationConf != null ||
                        task.subtasks.isNotEmpty() || task.isExpired()
                    ) {
                        TaskMetadata(task = task)
                    }
                }

                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun EnhancedSectionHeader(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 24.dp)
                .background(color, RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun TaskMetadata(task: Task) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        if (task.subtasks.isNotEmpty()) {
            EnhancedChip(
                icon = painterResource(R.drawable.ic_subtasks),
                iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                text = "${task.subtasks.count { it.isCompleted }}/${task.subtasks.size}"
            )
        }

        if (task.priority != Priority.NONE) {
            EnhancedChip(
                icon = painterResource(R.drawable.priority),
                iconTint = when (task.priority) {
                    Priority.HIGH -> MaterialTheme.colorScheme.error
                    Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    Priority.LOW -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                },
                text = task.priority.name.lowercase()
            )
        }

        task.durationConf?.let {
            EnhancedChip(
                icon = painterResource(R.drawable.ic_duration),
                iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                text = DateTimeFormatters.formatDurationShort(it)
            )
        }


        if (task.isExpired()) {
            EnhancedChip(
                icon = painterResource(R.drawable.expired),
                iconTint = MaterialTheme.colorScheme.error,
                text = "Expired"
            )
        }

        task.startDateConf?.let { startTimePlanning ->
            val dateText = buildString {

                append(DateTimeFormatters.formatDateShort(startTimePlanning))

                startTimePlanning.dateTime?.let { dateTime ->
                    if (dateTime.toLocalTime() != LocalTime.MIDNIGHT) {
                        append(" ${DateTimeFormatters.formatTime(dateTime)}")
                    }
                }
                startTimePlanning.dayPeriod?.takeIf { it != DayPeriod.NONE && it != DayPeriod.ALLDAY }
                    ?.let { period ->
                        append(" (${
                            period.name.lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        })"
                        )
                    }

                if (startTimePlanning.dayPeriod == DayPeriod.ALLDAY) {
                    append(" (All day)")
                }

                task.endDateConf?.let { endTimePlanning ->
                    append(" - ")

                    append(DateTimeFormatters.formatDateShort(endTimePlanning))

                    endTimePlanning.dateTime?.let { dateTime ->
                        if (dateTime.toLocalTime() != LocalTime.MIDNIGHT) {
                            append(" ${DateTimeFormatters.formatTime(dateTime)}")
                        }
                    }

                    endTimePlanning.dayPeriod?.takeIf {
                        it != DayPeriod.NONE && it != DayPeriod.ALLDAY && it != startTimePlanning.dayPeriod
                    }?.let { period ->
                        append(" (${period.name.lowercase().capitalize()})")
                    }
                }
            }
            EnhancedChip(
                icon = painterResource(R.drawable.ic_calendar),
                iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                text = dateText
            )
        }
    }
}


@Composable
fun EnhancedChip(
    icon: Painter,
    iconTint: Color,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = iconTint
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
