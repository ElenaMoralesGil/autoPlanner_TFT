package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Priority
import org.koin.androidx.compose.koinViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.intents.TaskFilter
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.ui.utils.ErrorMessage
import com.elena.autoplanner.presentation.ui.utils.FilterDropdown
import com.elena.autoplanner.presentation.utils.DateTimeFormatters
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator

/** TODO:
 * add indicator in taskCard when task has subtasks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var selectedTaskToEdit by remember { mutableStateOf<Task?>(null) }
    val filterStatusText = remember(state.selectedStatus, state.selectedTimeFrame) {
        buildString {
            if (state.selectedTimeFrame != TimeFrame.ALL) {
                append(state.selectedTimeFrame.displayName)
            }
            if (state.selectedStatus != TaskStatus.ALL) {
                append(" • ${state.selectedStatus.displayName}")
            }
        }.ifEmpty { "All Tasks" }
    }
    LaunchedEffect(Unit) {
        viewModel.triggerEvent(TaskIntent.LoadTasks)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Column {
                            Text(
                                "Tasks",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = filterStatusText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Status Filter Icon
                    var showStatusMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showStatusMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_filter),
                                contentDescription = "Status filter",
                                tint = if (state.selectedStatus != TaskStatus.ALL)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            TaskStatus.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.displayName) },
                                    onClick = {
                                        viewModel.triggerEvent(TaskIntent.UpdateStatusFilter(status))
                                        showStatusMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(
                                                when (status) {
                                                    TaskStatus.COMPLETED -> R.drawable.ic_completed
                                                    TaskStatus.UNCOMPLETED -> R.drawable.ic_uncompleted
                                                    else -> R.drawable.ic_lists
                                                }
                                            ),
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Time Filter Icon
                    var showTimeMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showTimeMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = "Time filter",
                                tint = if (state.selectedTimeFrame != TimeFrame.ALL)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showTimeMenu,
                            onDismissRequest = { showTimeMenu = false }
                        ) {
                            TimeFrame.entries.forEach { timeFrame ->
                                DropdownMenuItem(
                                    text = { Text(timeFrame.displayName) },
                                    onClick = {
                                        viewModel.triggerEvent(TaskIntent.UpdateTimeFrameFilter(timeFrame))
                                        showTimeMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(
                                                when (timeFrame) {
                                                    TimeFrame.TODAY -> R.drawable.ic_lists
                                                    TimeFrame.WEEK -> R.drawable.ic_lists
                                                    TimeFrame.MONTH -> R.drawable.ic_lists
                                                    else -> R.drawable.ic_calendar
                                                }
                                            ),
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val interactionSource = remember { MutableInteractionSource() }
            val animatedElevation by animateDpAsState(
                targetValue = if (interactionSource.collectIsPressedAsState().value) 4.dp else 8.dp,
                label = "fabElevation"
            )

            FloatingActionButton(
                onClick = { showAddTaskSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                interactionSource = interactionSource,
                modifier = Modifier
                    .shadow(
                        elevation = animatedElevation,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = MaterialTheme.colorScheme.primary
                    )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Task",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when {
                state.isLoading -> LoadingIndicator()
                state.error != null -> ErrorMessage(state.error!!)
                state.filteredTasks.isEmpty() -> EmptyState(onAddTask = { showAddTaskSheet = true })
                else -> TaskCardList(
                    tasks = state.filteredTasks,
                    onTaskChecked = { task, checked ->
                        viewModel.triggerEvent(TaskIntent.ToggleTaskCompletion(task, checked))
                    },
                    viewModel = viewModel,
                    selectedTaskToEdit = selectedTaskToEdit,
                    onTaskSelected = { task -> selectedTaskToEdit = task }
                )
            }
        }
    }

    selectedTaskToEdit?.let { task ->
        TaskDetailSheet(
            task = task,
            onDismiss = { selectedTaskToEdit = null },
            onDelete = {
                viewModel.triggerEvent(TaskIntent.DeleteTask(task))
                selectedTaskToEdit = null
            },
            onEdit = { /* Handle edit navigation */ },
            onSubtaskAdded = { subtaskName ->
                viewModel.triggerEvent(TaskIntent.AddSubtask(task, subtaskName))
            },
            onSubtaskToggled = { subtask, checked ->
                viewModel.triggerEvent(TaskIntent.ToggleSubtask(task, subtask, checked))
            }
        )
    }

    if (showAddTaskSheet) {
        AddTaskSheet(
            onClose = { showAddTaskSheet = false },
            onAccept = { newTaskData ->
                viewModel.triggerEvent(TaskIntent.CreateTask(newTaskData))
                showAddTaskSheet = false
            }
        )
    }
}

@Composable
private fun EmptyState(onAddTask: () -> Unit) {
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
private fun TaskCardList(
    tasks: List<Task>,
    onTaskChecked: (Task, Boolean) -> Unit,
    viewModel: TaskViewModel,  // Add viewModel parameter
    selectedTaskToEdit: Task?,  // Add selectedTask parameter
    onTaskSelected: (Task) -> Unit  // Add selection callback
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(tasks) { task ->
            TaskCard(
                task = task,
                onCheckedChange = { checked -> onTaskChecked(task, checked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTaskSelected(task) }
            )
        }
    }
}


@Composable
fun SquareCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(checked, label = "checkbox")
    val borderColor by transition.animateColor(label = "border") { isChecked ->
        if (isChecked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline
    }
    val checkAlpha by transition.animateFloat(label = "checkAlpha") { if (it) 1f else 0f }

    Box(
        modifier = modifier
            .size(24.dp)
            .clickable { onCheckedChange(!checked) }
            .border(2.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = checkAlpha),
            modifier = Modifier.size(18.dp)
        )
    }
}

// Enhanced Label Chip Component
@Composable
fun LabelChip(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Improved Task Card
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskCard(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Priority Indicator
            Box(
                modifier = Modifier
                    .size(4.dp, 32.dp)
                    .background(
                        color = when (task.priority) {
                            Priority.HIGH -> MaterialTheme.colorScheme.error
                            Priority.MEDIUM -> Color(0xFFFFC107)
                            Priority.LOW -> Color(0xFF4CAF50)
                            else -> Color.Transparent
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    task.startDateConf?.let {
                        LabelChip(
                            icon = painterResource(R.drawable.ic_calendar),
                            text = DateTimeFormatters.formatDateShort(it)
                        )
                    }

                    task.durationConf?.let {
                        LabelChip(
                            icon = painterResource(R.drawable.ic_duration),
                            text = DateTimeFormatters.formatDurationShort(it)
                        )
                    }
                }
            }

            SquareCheckbox(
                checked = task.isCompleted,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun FilterSection(
    selectedStatus: TaskStatus,
    selectedTimeFrame: TimeFrame,
    onStatusChanged: (TaskStatus) -> Unit,
    onTimeFrameChanged: (TimeFrame) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Filter by Status", style = MaterialTheme.typography.labelLarge)
        StatusFilterRow(selectedStatus, onStatusChanged)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Filter by Time", style = MaterialTheme.typography.labelLarge)
        TimeFilterRow(selectedTimeFrame, onTimeFrameChanged)
    }
}
@Composable
private fun StatusFilterRow(
    selectedStatus: TaskStatus,
    onStatusChanged: (TaskStatus) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskStatus.values().forEach { status ->
            EnhancedFilterChip(
                text = status.displayName,
                selected = status == selectedStatus,
                onClick = { onStatusChanged(status) }
            )
        }
    }
}
@Composable
private fun TimeFilterRow(
    selectedTimeFrame: TimeFrame,
    onTimeFrameChanged: (TimeFrame) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeFrame.values().forEach { timeFrame ->
            EnhancedFilterChip(
                text = timeFrame.displayName,
                selected = timeFrame == selectedTimeFrame,
                onClick = { onTimeFrameChanged(timeFrame) }
            )
        }
    }
}

@Composable
private fun EnhancedFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            iconColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderWidth = 1.dp,
            enabled = true,
            selected = selected
        ),
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null,
        label = {
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    )
}
