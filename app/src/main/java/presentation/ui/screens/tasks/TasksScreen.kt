package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
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


@Composable
fun TasksScreen(viewModel: TaskViewModel = koinViewModel()) {
    // El estado global del ViewModel (lista de tareas, filtros, etc.)
    val state by viewModel.state.collectAsState()

    // Controladores de hoja
    var showAddEditSheet by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    val detailSubtaskListState = rememberLazyListState()
    // Esta variable conservará la tarea concreta que se va a editar
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    // Al iniciar la pantalla, cargamos las tareas
    LaunchedEffect(Unit) {
        viewModel.sendIntent(TaskIntent.LoadTasks)
    }

    // Estructura principal de la pantalla (TopBar, FAB, contenido, etc.)
    Scaffold(
        topBar = {
            state?.let {
                TasksAppBar(
                    state = it,
                    onStatusSelected = { status -> viewModel.sendIntent(TaskIntent.UpdateStatusFilter(status)) },
                    onTimeFrameSelected = { tf -> viewModel.sendIntent(TaskIntent.UpdateTimeFrameFilter(tf)) }
                )
            }
        },
        floatingActionButton = {
            AddTaskFAB {
                // Modo crear tarea (sin ninguna seleccionada)
                taskToEdit = null // Asegura que no habrá datos previos
                showAddEditSheet = true
            }
        },
        content = { innerPadding ->
            state?.let { currentState ->
                ContentContainer(
                    state = currentState,
                    innerPadding = innerPadding,
                    onTaskChecked = { task, checked ->
                        viewModel.sendIntent(TaskIntent.ToggleTaskCompletion(task, checked))
                    },
                    onTaskSelected = { task ->
                        // Al hacer clic en una tarjeta de la lista, se abre la hoja de detalle
                        selectedTaskId = task.id
                    },
                    onAddTask = {
                        // También se puede acceder aquí, si se quisiera
                        taskToEdit = null
                        showAddEditSheet = true
                    }
                )
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
                // Se elimina la tarea y se cierra la hoja de detalle
                viewModel.sendIntent(TaskIntent.DeleteTask(taskId))
                selectedTaskId = null
            },
            onSubtaskToggled = { subtaskId, isCompleted ->
                viewModel.sendIntent(TaskIntent.ToggleSubtask(taskId, subtaskId, isCompleted))
            },
            onEdit = {
                // 1) Localizar la tarea que se está mostrando
                val foundTask = state?.tasks?.find { it.id == taskId }
                // 2) Guardarla para editarla en la otra hoja
                taskToEdit = foundTask
                // 3) Cerrar la hoja de detalle
                selectedTaskId = null
                // 4) Abrir la hoja de edición
                showAddEditSheet = true
            }
        )
    }

    if (showAddEditSheet) {
        AddEditTaskSheet(
            taskToEdit = taskToEdit,
            onClose = {
                // Close the edit sheet
                showAddEditSheet = false
                // If we were editing (not creating), reopen the detail sheet
                if (taskToEdit != null) {
                    selectedTaskId = taskToEdit?.id
                }
                // Clear the edit reference
                taskToEdit = null
            },
            onCreateTask = { newTaskData ->
                viewModel.sendIntent(TaskIntent.CreateTask(newTaskData))
                showAddEditSheet = false
            },
            onUpdateTask = { updatedTask ->
                viewModel.sendIntent(TaskIntent.UpdateTask(updatedTask))
                showAddEditSheet = false
                // Reopen detail view after update
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
private fun ContentContainer(
    state: TaskState,
    innerPadding: PaddingValues,
    onTaskChecked: (Task, Boolean) -> Unit,
    onTaskSelected: (Task) -> Unit,
    onAddTask: () -> Unit
) {
    Column(modifier = Modifier.padding(innerPadding)) {
        when (val uiState = state.uiState) {
            TaskState.UiState.Loading -> LoadingIndicator()
            is TaskState.UiState.Error -> uiState.message?.let { ErrorMessage(it) }
            else -> {
                if (state.filteredTasks.isEmpty()) {
                    EmptyState(onAddTask)
                } else {
                    TaskCardList(
                        tasks = state.filteredTasks,
                        onTaskChecked = onTaskChecked,
                        onTaskSelected = onTaskSelected
                    )
                }
            }
        }
    }
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

// Extension functions for icon resources
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
    onTaskSelected: (Task) -> Unit
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
            // Indicador de prioridad
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

            // Contenido principal
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

                // Muestra chips de fecha, duración, subtareas, etc.
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

                    if (task.subtasks.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_subtasks),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${task.subtasks.count { it.isCompleted }}/${task.subtasks.size}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Checkbox personalizado para completar la tarea
            SquareCheckbox(
                checked = task.isCompleted,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

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

@Composable
fun SquareCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clickable { onCheckedChange(!checked) }
            .background(Color.Transparent)
    ) {
        // Borde
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent, RoundedCornerShape(4.dp))
                .border(
                    width = 2.dp,
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
        )

        // Ícono de check
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.Center)
            )
        }
    }
}
