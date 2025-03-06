package com.elena.autoplanner.presentation.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.isToday
import com.elena.autoplanner.presentation.effects.CalendarEffect
import com.elena.autoplanner.presentation.effects.TaskDetailEffect
import com.elena.autoplanner.presentation.effects.TaskEditEffect
import com.elena.autoplanner.presentation.intents.CalendarIntent
import com.elena.autoplanner.presentation.intents.TaskEditIntent
import com.elena.autoplanner.presentation.intents.TaskListIntent
import com.elena.autoplanner.presentation.ui.screens.calendar.MonthlyView.MonthlyView
import com.elena.autoplanner.presentation.ui.screens.calendar.WeeklyView.WeeklyView
import com.elena.autoplanner.presentation.ui.screens.tasks.ModificationTaskSheet.ModificationTaskSheet
import com.elena.autoplanner.presentation.ui.screens.tasks.TaskDetailSheet
import com.elena.autoplanner.presentation.ui.utils.CustomCalendar
import com.elena.autoplanner.presentation.viewmodel.CalendarViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskListViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class CalendarView { DAY, WEEK, MONTH }

@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel = koinViewModel(),
    taskListViewModel: TaskListViewModel = koinViewModel()
) {
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var showAddEditSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showViewSelector by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val onTaskSelected: (Task) -> Unit = { task -> selectedTaskId = task.id }

    val calendarState by calendarViewModel.state.collectAsState()
    val tasksState by taskListViewModel.state.collectAsState()

    // Handle effects from CalendarViewModel
    LaunchedEffect(calendarViewModel) {
        calendarViewModel.effect.collectLatest { effect ->
            when (effect) {
                is CalendarEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is CalendarEffect.NavigateTo -> {

                }

                is CalendarEffect.ShowLoading -> {

                }

                is CalendarEffect.Error -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is CalendarEffect.Success -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is CalendarEffect.SwitchView -> {

                }
            }
        }
    }

    // Load tasks when screen is displayed
    LaunchedEffect(Unit) {
        taskListViewModel.sendIntent(TaskListIntent.LoadTasks)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier,
        topBar = {
            calendarState?.let {
                CalendarTopAppBar(
                    currentDate = it.currentDate,
                    currentView = it.currentView,
                    onViewSelectorClicked = { showViewSelector = !showViewSelector },
                    onTitleSelected = {
                        calendarViewModel.sendIntent(CalendarIntent.ToggleDatePicker(true))
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            calendarState?.let { state ->
                when (state.currentView) {
                    CalendarView.DAY -> tasksState?.tasks?.let { tasks ->
                        DailyView(
                            selectedDate = state.currentDate,
                            tasks = tasks.filter { it.isDueOn(state.currentDate) },
                            onTaskSelected = onTaskSelected,
                            calendarViewModel = calendarViewModel,
                            tasksViewModel = taskListViewModel
                        )
                    }

                    CalendarView.WEEK -> tasksState?.tasks?.let { tasks ->
                        WeeklyView(
                            tasks = tasks,
                            onTaskSelected = onTaskSelected,
                            calendarViewModel = calendarViewModel,
                            tasksViewModel = taskListViewModel,
                            weekStartDate = state.currentDate
                        )
                    }

                    CalendarView.MONTH -> tasksState?.tasks?.let { tasks ->
                        MonthlyView(
                            selectedMonth = YearMonth.from(state.currentDate),
                            tasks = tasks,
                            onTaskSelected = onTaskSelected,
                            calendarViewModel = calendarViewModel
                        )
                    }
                }
            }
        }

        // Date picker dialog
        calendarState?.let { state ->
            if (state.showDatePicker) {
                Dialog(
                    onDismissRequest = {
                        calendarViewModel.sendIntent(CalendarIntent.ToggleDatePicker(false))
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 8.dp
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            CustomCalendar(
                                currentMonth = YearMonth.from(state.currentDate),
                                selectedDates = listOf(state.currentDate),
                                showNavigation = true,
                                onDateSelected = { selectedDate ->
                                    calendarViewModel.sendIntent(
                                        CalendarIntent.ChangeDate(selectedDate, dismiss = true)
                                    )
                                },
                                onMonthChanged = { newMonth ->
                                    calendarViewModel.sendIntent(
                                        CalendarIntent.ChangeDate(
                                            newMonth.atDay(1),
                                            dismiss = false
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Task detail sheet
        selectedTaskId?.let { taskId ->
            // Create a TaskDetailViewModel for the selected task
            val detailViewModel: TaskDetailViewModel =
                koinViewModel(parameters = { parametersOf(taskId) })

            // Handle effects from the detail ViewModel
            LaunchedEffect(detailViewModel) {
                detailViewModel.effect.collectLatest { effect ->
                    when (effect) {
                        is TaskDetailEffect.NavigateBack -> {
                            selectedTaskId = null
                            // Refresh the task list
                            taskListViewModel.sendIntent(TaskListIntent.LoadTasks)
                        }

                        is TaskDetailEffect.NavigateToEdit -> {
                            taskToEdit = tasksState?.tasks?.find { it.id == effect.taskId }
                            selectedTaskId = null
                            showAddEditSheet = true
                        }

                        is TaskDetailEffect.ShowSnackbar -> {
                            snackbarHostState.showSnackbar(effect.message)
                        }
                    }
                }
            }

            // Show the TaskDetailSheet
            TaskDetailSheet(
                taskId = taskId,
                onDismiss = { selectedTaskId = null },
                viewModel = detailViewModel
            )
        }

        // View selector dropdown
        if (showViewSelector) {
            ViewSelector(
                currentView = calendarState?.currentView ?: CalendarView.DAY,
                onViewSelected = { view ->
                    calendarViewModel.sendIntent(CalendarIntent.ChangeView(view))
                    showViewSelector = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp)
            )
        }

        // Add/edit task sheet
        if (showAddEditSheet) {
            // Create a TaskEditViewModel for editing
            val editViewModel: TaskEditViewModel =
                koinViewModel(parameters = { parametersOf(taskToEdit?.id ?: 0) })

            // Handle effects from the edit ViewModel
            LaunchedEffect(editViewModel) {
                editViewModel.effect.collectLatest { effect ->
                    when (effect) {
                        is TaskEditEffect.NavigateBack -> {
                            showAddEditSheet = false
                            if (taskToEdit != null) {
                                selectedTaskId = taskToEdit?.id
                            }
                            taskToEdit = null
                            // Refresh the task list
                            taskListViewModel.sendIntent(TaskListIntent.LoadTasks)
                        }

                        is TaskEditEffect.ShowSnackbar -> {
                            snackbarHostState.showSnackbar(effect.message)
                        }
                    }
                }
            }

            // Initialize the edit ViewModel with the task to edit
            LaunchedEffect(taskToEdit?.id) {
                editViewModel.sendIntent(TaskEditIntent.LoadTask(taskToEdit?.id ?: 0))
            }

            // Show the ModificationTaskSheet
            ModificationTaskSheet(
                taskToEdit = taskToEdit,
                taskEditViewModel = editViewModel,
                onClose = { editViewModel.sendIntent(TaskEditIntent.Cancel) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopAppBar(
    currentDate: LocalDate,
    currentView: CalendarView,
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