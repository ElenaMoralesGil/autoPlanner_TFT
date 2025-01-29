package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elena.autoplanner.R
import org.koin.androidx.compose.koinViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.intents.TaskFilter
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.ui.utils.ErrorMessage
import com.elena.autoplanner.presentation.ui.utils.FilterDropdown
import com.elena.autoplanner.presentation.utils.DateTimeFormatters
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator


// TasksScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var selectedTaskToEdit by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(Unit) {
        viewModel.triggerEvent(TaskIntent.LoadTasks)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Tasks", fontSize = 20.sp) },
                    actions = {
                        FilterDropdown(
                            currentFilter = state.currentFilter,
                            onFilterSelected = { filter ->
                                viewModel.triggerEvent(TaskIntent.UpdateFilter(filter))
                            }
                        )
                    }
                )
                FilterChipsRow(
                    currentFilter = state.currentFilter,
                    onFilterSelected = { filter ->
                        viewModel.triggerEvent(TaskIntent.UpdateFilter(filter))
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when {
                state.isLoading -> LoadingIndicator()
                state.error != null -> ErrorMessage(error = state.error!!)
                else -> TaskList(
                    tasks = state.filteredTasks,
                    onTaskChecked = { task, checked ->
                        viewModel.triggerEvent(TaskIntent.ToggleTaskCompletion(task, checked))
                    },
                    onTaskClicked = { task ->
                        selectedTaskToEdit = task
                    }
                )
            }
        }
    }

    selectedTaskToEdit?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { selectedTaskToEdit = null },
            onDelete = {
                viewModel.triggerEvent(TaskIntent.DeleteTask(task))
                selectedTaskToEdit = null
            },
            onSave = { updatedTask ->
                viewModel.triggerEvent(TaskIntent.UpdateTask(updatedTask))
                selectedTaskToEdit = null
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
private fun FilterChipsRow(
    currentFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskFilter.entries.forEach { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) },
                leadingIcon = if (currentFilter == filter) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}
@Composable
fun DetailItem(icon: Painter, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
@Composable
private fun TaskList(
    tasks: List<Task>,
    onTaskChecked: (Task, Boolean) -> Unit,
    onTaskClicked: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(
            items = tasks,
            key = { it.id }
        ) { task ->
            TaskCard(
                task = task,
                onCheckedChange = { checked -> onTaskChecked(task, checked) },
                onClick = { onTaskClicked(task) }
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )

                Column(modifier = Modifier.weight(1f)) {
                    TaskTitle(task)
                    TaskDetails(task)
                }

                TaskStatusIndicators(task)
            }

            if (task.subtasks.isNotEmpty()) {
                SubtaskProgress(subtasks = task.subtasks)
            }
        }
    }
}

@Composable
private fun TaskTitle(task: Task) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = task.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        PriorityIndicator(priority = task.priority)
    }
}

@Composable
private fun TaskDetails(task: Task) {
    val formatter = DateTimeFormatters

    Column {
        task.startDateConf?.let {
            DetailItem(
                icon = painterResource(R.drawable.ic_calendar),
                text = formatter.formatDateTimeWithPeriod(it)
            )
        }

        task.endDateConf?.let {
            DetailItem(
                icon = painterResource(R.drawable.ic_calendar),
                text = formatter.formatDateTimeWithPeriod(it)
            )
        }

        task.durationConf?.let {
            DetailItem(
                icon = painterResource(R.drawable.ic_duration),
                text = formatter.formatDurationForDisplay(it)
            )
        }
    }
}

@Composable
private fun TaskStatusIndicators(task: Task) {
    Column {
        if (task.isExpired && !task.isCompleted) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = "Expired",
                tint = MaterialTheme.colorScheme.error
            )
        }
        if (task.repeatPlan != null) {
            Icon(
                painter = painterResource(R.drawable.ic_repeat),
                contentDescription = "Repeating",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}






