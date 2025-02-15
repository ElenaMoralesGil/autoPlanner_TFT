package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.states.TaskSection
import com.elena.autoplanner.presentation.states.TaskState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.ui.utils.ErrorMessage
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator
import com.elena.autoplanner.presentation.utils.DateTimeFormatters
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs
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
                            if (currentState.filteredTasks.isEmpty()) {
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
                                    onDragEnd = { task, targetSection ->
                                        when (targetSection) {
                                            TaskSection.COMPLETED -> viewModel.sendIntent(
                                                TaskIntent.UpdateTask(
                                                    task.copy(
                                                        isCompleted = true,
                                                        isExpired = false
                                                    )
                                                )
                                            )

                                            TaskSection.EXPIRED -> viewModel.sendIntent(
                                                TaskIntent.UpdateTask(task.copy(isExpired = true))
                                            )

                                            TaskSection.NOT_DONE -> {
                                                viewModel.sendIntent(
                                                    TaskIntent.UpdateTask(
                                                        task.copy(
                                                            isCompleted = false,
                                                            isExpired = false
                                                        )
                                                    )
                                                )
                                            }
                                        }
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
        title = {
            Column {
                Text("Tasks", style = MaterialTheme.typography.headlineSmall)
                Text(buildFilterText(state), style = MaterialTheme.typography.bodyLarge)
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
                else MaterialTheme.colorScheme.onSurface
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
    onDragEnd: (Task, TaskSection) -> Unit
) {
    val tasks = state.filteredTasks
    val notDoneTasks = tasks.filter { !it.isCompleted && !it.isExpired }
    val completedTasks = tasks.filter { it.isCompleted && !it.isExpired }
    val expiredTasks = tasks.filter { it.isExpired }

    // Only show relevant sections based on filters
    val showNotDone = when (state.filters.status) {
        TaskStatus.ALL, TaskStatus.UNCOMPLETED -> true
        else -> false
    }

    val showCompleted = when (state.filters.status) {
        TaskStatus.ALL, TaskStatus.COMPLETED -> true
        else -> false
    }

    val showExpired = state.filters.timeFrame == TimeFrame.EXPIRED

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (showNotDone && notDoneTasks.isNotEmpty()) {
            stickyHeader {
                EnhancedSectionHeader(
                    title = "Not Done",
                    count = notDoneTasks.size,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(notDoneTasks) { task ->
                AnimatedDraggableTaskCard(
                    task = task,
                    onDragEnd = { targetSection -> onDragEnd(task, targetSection) },
                    onCheckedChange = { checked -> onTaskChecked(task, checked) },
                    onTaskSelected = { onTaskSelected(task) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (showCompleted && completedTasks.isNotEmpty()) {
            stickyHeader {
                EnhancedSectionHeader(
                    title = "Completed",
                    count = completedTasks.size,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            items(completedTasks) { task ->
                AnimatedDraggableTaskCard(
                    task = task,
                    onDragEnd = { targetSection -> onDragEnd(task, targetSection) },
                    onCheckedChange = { checked -> onTaskChecked(task, checked) },
                    onTaskSelected = { onTaskSelected(task) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (showExpired && expiredTasks.isNotEmpty()) {
            stickyHeader {
                EnhancedSectionHeader(
                    title = "Expired",
                    count = expiredTasks.size,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(expiredTasks) { task ->
                AnimatedDraggableTaskCard(
                    task = task,
                    onDragEnd = { targetSection -> onDragEnd(task, targetSection) },
                    onCheckedChange = { checked -> onTaskChecked(task, checked) },
                    onTaskSelected = { onTaskSelected(task) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AnimatedDraggableTaskCard(
    task: Task,
    onDragEnd: (TaskSection) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onTaskSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    val dragThreshold = 100f
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val maxOffset = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .pointerInput(task.id) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(-maxOffset, maxOffset)
                    },
                    onDragEnd = {
                        isDragging = false
                        val targetSection = when {
                            offsetX > dragThreshold -> TaskSection.COMPLETED
                            offsetX < -dragThreshold -> TaskSection.EXPIRED
                            else -> TaskSection.NOT_DONE
                        }
                        onDragEnd(targetSection)
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                        isDragging = false
                    }
                )
            }
    ) {
        // Drag direction indicators
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer
                            .copy(alpha = offsetX.coerceIn(0f, 1f))
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (offsetX > 0) {
                    Text("Complete", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer
                            .copy(alpha = abs(offsetX).coerceIn(0f, 1f))
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                if (offsetX < 0) {
                    Text("Expired", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        EnhancedTaskCard(
            task = task,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .clickable { onTaskSelected() }
                .graphicsLayer {
                    val scale = 1f - (abs(offsetX) * 0.002f)
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    4.dp,
                    RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.outline
                )
        )
    }
}

@Composable
fun EnhancedTaskCard(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Priority indicator
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
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    ),
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                if (task.startDateConf != null || task.durationConf != null || task.subtasks.isNotEmpty()) {
                    TaskMetadata(task = task)
                }
            }

            // Enhanced checkbox using Material3 Surface
            Surface(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp)),
                onClick = { onCheckedChange(!task.isCompleted) },
                shape = RoundedCornerShape(6.dp),
                color = if (task.isCompleted)
                    MaterialTheme.colorScheme.primary
                else
                    Color.Transparent,
                border = BorderStroke(
                    width = 2.dp,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                if (task.isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxSize()
                    )
                }
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
            style = MaterialTheme.typography.titleMedium,
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
private fun PriorityIndicator(priority: Priority) {
    val color = when (priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.error
        Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
        Priority.LOW -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    EnhancedChip(
        icon = painterResource(R.drawable.priority),
        iconTint = color,
        text = priority.name.lowercase(),
    )
}

@Composable
private fun TaskMetadata(task: Task) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        task.startDateConf?.let {
            EnhancedChip(
                icon = painterResource(R.drawable.ic_calendar),
                iconTint = MaterialTheme.colorScheme.primary,
                text = DateTimeFormatters.formatDateShort(it)
            )
        }

        task.durationConf?.let {
            EnhancedChip(
                icon = painterResource(R.drawable.ic_duration),
                iconTint = MaterialTheme.colorScheme.primary,
                text = DateTimeFormatters.formatDurationShort(it)
            )
        }

        if (task.subtasks.isNotEmpty()) {
            EnhancedChip(
                icon = painterResource(R.drawable.ic_subtasks),
                iconTint = MaterialTheme.colorScheme.primary,
                text = "${task.subtasks.count { it.isCompleted }}/${task.subtasks.size}"
            )
        }

        if (task.priority != Priority.NONE) {
            PriorityIndicator(task.priority)
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

@Composable
private fun EnhancedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clickable { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 2.dp,
                    color = if (checked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            if (checked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxSize()
                )
            }
        }
    }
}
