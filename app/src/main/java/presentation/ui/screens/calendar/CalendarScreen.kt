package com.elena.autoplanner.presentation.ui.screens.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.isToday
import com.elena.autoplanner.presentation.intents.CalendarIntent
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.ui.screens.tasks.AddEditTaskSheet
import com.elena.autoplanner.presentation.ui.screens.tasks.TaskDetailSheet
import com.elena.autoplanner.presentation.ui.utils.CustomCalendar
import com.elena.autoplanner.presentation.ui.utils.WeekHeader
import com.elena.autoplanner.presentation.viewmodel.CalendarViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter


enum class CalendarView { DAY, WEEK, MONTH }

@Composable
fun CalendarScreen(
    taskViewModel: TaskViewModel = koinViewModel(),
    calendarViewModel: CalendarViewModel = koinViewModel()
) {
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var showAddEditSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showViewSelector by remember { mutableStateOf(false) }
    val onTaskSelected: (Task) -> Unit = { task -> selectedTaskId = task.id }

    val calendarState by calendarViewModel.state.collectAsState()
    val taskState by taskViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        taskViewModel.sendIntent(TaskIntent.LoadTasks)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier,
        topBar = {
            CalendarTopAppBar(
                currentDate = calendarState.currentDate,
                currentView = calendarState.currentView,
                onViewChanged = {
                    calendarViewModel.processIntent(CalendarIntent.ChangeView(it))
                    showViewSelector = false
                },
                onViewSelectorClicked = { showViewSelector = !showViewSelector },
                onTitleSelected = {
                    calendarViewModel.processIntent(
                        CalendarIntent.ToggleDatePicker(
                            true
                        )
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
        ) {
            when (calendarState.currentView) {
                CalendarView.DAY -> taskState?.tasks?.let { tasks ->
                    DailyView(
                        selectedDate = calendarState.currentDate,
                        tasks = tasks.filter { it.isDueOn(calendarState.currentDate) },
                        onTaskSelected = onTaskSelected,
                        calendarViewModel = calendarViewModel,
                        taskViewModel = taskViewModel
                    )
                }

                CalendarView.WEEK -> taskState?.tasks?.let { tasks ->
                    WeeklyView(
                        weekStartDate = calendarState.currentDate,
                        tasks = tasks,
                        onTaskSelected = onTaskSelected,
                        calendarViewModel = calendarViewModel
                    )
                }

                CalendarView.MONTH -> taskState?.tasks?.let { tasks ->
                }
            }
        }



        if (calendarState.showDatePicker) {
            Dialog(
                onDismissRequest = {
                    calendarViewModel.processIntent(
                        CalendarIntent.ToggleDatePicker(false)
                    )
                }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        CustomCalendar(
                            currentMonth = YearMonth.from(calendarState.currentDate),
                            selectedDates = listOf(calendarState.currentDate),
                            showNavigation = true,
                            onDateSelected = { selectedDate ->
                                calendarViewModel.processIntent(
                                    CalendarIntent.ChangeDate(selectedDate, dismiss = true)
                                )
                            },
                            onMonthChanged = { newMonth ->
                                calendarViewModel.processIntent(
                                    CalendarIntent.ChangeDate(newMonth.atDay(1), dismiss = false)
                                )
                            }
                        )
                    }
                }
            }
        }

        selectedTaskId?.let { taskId ->
            TaskDetailSheet(
                taskId = taskId,
                onDismiss = {
                    selectedTaskId = null
                },
                viewModel = taskViewModel,
                onSubtaskDeleted = { subtask ->
                    taskViewModel.sendIntent(TaskIntent.DeleteSubtask(taskId, subtask.id))
                },
                onSubtaskAdded = { subtaskName ->
                    taskViewModel.sendIntent(TaskIntent.AddSubtask(taskId, subtaskName))
                },
                onDelete = {
                    taskViewModel.sendIntent(TaskIntent.DeleteTask(taskId))
                    selectedTaskId = null
                },
                onSubtaskToggled = { subtaskId, isCompleted ->
                    taskViewModel.sendIntent(
                        TaskIntent.ToggleSubtask(
                            taskId,
                            subtaskId,
                            isCompleted
                        )
                    )
                },
                onEdit = {
                    taskToEdit = taskState?.tasks?.find { it.id == taskId }
                    selectedTaskId = null
                    showAddEditSheet = true
                }
            )
        }

        if (showViewSelector) {
            ViewSelector(
                currentView = calendarState.currentView,
                onViewSelected = { view ->
                    calendarViewModel.processIntent(CalendarIntent.ChangeView(view))
                    showViewSelector = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp)
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
                    taskViewModel.sendIntent(TaskIntent.CreateTask(newTaskData))
                    showAddEditSheet = false
                },
                onUpdateTask = { updatedTask ->
                    taskViewModel.sendIntent(TaskIntent.UpdateTask(updatedTask))
                    showAddEditSheet = false
                    selectedTaskId = updatedTask.id
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopAppBar(
    currentDate: LocalDate,
    currentView: CalendarView,
    onViewChanged: (CalendarView) -> Unit,
    onTitleSelected: () -> Unit,
    onViewSelectorClicked: () -> Unit
) {
    val titleText = when (currentView) {
        CalendarView.DAY -> {
            if (currentDate.isToday()) {
                "Today"
            } else {
                currentDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
            }
        }

        CalendarView.WEEK ->
            "Week of ${currentDate.format(DateTimeFormatter.ofPattern("d MMM"))}"

        CalendarView.MONTH ->
            currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    TopAppBar(
        title = {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clickable { onTitleSelected() }
            )
        },
        actions = {
            IconButton(onClick = onViewSelectorClicked) {
                Icon(
                    painter = painterResource(currentView.getIconRes()),
                    contentDescription = "View Selector"
                )
            }
        }
    )
}

@Composable
private fun ViewSelector(
    currentView: CalendarView,
    onViewSelected: (CalendarView) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CalendarView.entries.forEach { view ->
                ViewOption(
                    view = view,
                    isSelected = view == currentView,
                    onSelected = { onViewSelected(view) }
                )
            }
        }
    }
}


@Composable
private fun ViewOption(
    view: CalendarView,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val tintColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .clickable(onClick = onSelected)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(view.getIconRes()),
            contentDescription = view.name,
            tint = tintColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = view.name,
            color = tintColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

fun CalendarView.getIconRes(): Int = when (this) {
    CalendarView.DAY -> R.drawable.ic_day_view
    CalendarView.WEEK -> R.drawable.ic_week_view
    CalendarView.MONTH -> R.drawable.ic_month_view
}


data class TaskGroup(
    var endTime: LocalTime,
    val tasks: MutableList<Task>
)


fun LocalTime.toMinutes(): Int = this.hour * 60 + this.minute


@Composable
private fun WeeklyView(
    weekStartDate: LocalDate,
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit,
    calendarViewModel: CalendarViewModel
) {
    val weekDays = calendarViewModel.getWeekDays(weekStartDate)

    Column(modifier = Modifier.fillMaxSize()) {
        // Week header with selectable days
        WeekHeader(
            selectedDate = weekStartDate,
            onDateSelected = { newDate ->
                calendarViewModel.processIntent(CalendarIntent.ChangeDate(newDate))
            },
            modifier = Modifier.padding(8.dp)
        )

        // Weekly tasks list
        LazyColumn(modifier = Modifier.padding(8.dp)) {
            items(weekDays) { date ->
                DailyTaskList(
                    date = date,
                    tasks = tasks.filter { it.isDueOn(date) },
                    onTaskSelected = onTaskSelected
                )
            }
        }
    }
}

@Composable
private fun DailyTaskList(
    date: LocalDate,
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Date header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (date.isToday()) "Today" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Task items
            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks scheduled",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                tasks.forEach { task ->
                    CalendarTaskItem(
                        task = task,
                        onTaskSelected = onTaskSelected,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
@Composable
private fun CalendarTaskItem(
    task: Task,
    onTaskSelected: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .draggable(
                state = rememberDraggableState { },
                onDragStarted = { isDragging = true },
                onDragStopped = { isDragging = false },
                orientation = Orientation.Vertical
            )
            .background(
                color = if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onTaskSelected(task) }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = task.name, style = MaterialTheme.typography.bodyMedium)
            if (!task.isAllDay()) {
                Text(
                    text = "${task.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${
                        task.endTime.format(
                            DateTimeFormatter.ofPattern("HH:mm")
                        )
                    }",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

