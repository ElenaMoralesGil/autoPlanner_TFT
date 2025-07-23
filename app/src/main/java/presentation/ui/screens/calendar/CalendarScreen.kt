package com.elena.autoplanner.presentation.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
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
import com.elena.autoplanner.presentation.ui.screens.tasks.TaskDetailSheet
import com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet.ModificationTaskSheet
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
    taskListViewModel: TaskListViewModel = koinViewModel(),
) {
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var selectedInstanceIdentifier by remember { mutableStateOf<String?>(null) }
    var showAddEditSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showViewSelector by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val onTaskSelected: (Task) -> Unit = { task ->
        selectedTaskId = task.id
        selectedInstanceIdentifier = task.instanceIdentifier
    }

    val calendarState by calendarViewModel.state.collectAsState()
    val tasksState by taskListViewModel.state.collectAsState()

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

    // NUEVO: Efecto para cargar tareas cuando cambie la fecha o vista del calendario
    LaunchedEffect(calendarState?.currentDate, calendarState?.currentView) {
        calendarState?.let { state ->
            val (startDate, endDate) = when (state.currentView) {
                CalendarView.DAY -> {
                    // Para vista diaria: solo el día actual
                    state.currentDate to state.currentDate
                }

                CalendarView.WEEK -> {
                    // Para vista semanal: toda la semana
                    val startOfWeek = state.currentDate.with(
                        java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)
                    )
                    val endOfWeek = startOfWeek.plusDays(6)
                    startOfWeek to endOfWeek
                }

                CalendarView.MONTH -> {
                    // Para vista mensual: todo el mes
                    val yearMonth = java.time.YearMonth.from(state.currentDate)
                    yearMonth.atDay(1) to yearMonth.atEndOfMonth()
                }
            }

            taskListViewModel.sendIntent(TaskListIntent.LoadTasksForDateRange(startDate, endDate))
        }
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
                val taskDetailViewModel: TaskDetailViewModel =
                    koinViewModel() // Obtener VM para el filtro
                when (state.currentView) {
                    CalendarView.DAY -> tasksState?.tasks?.let { tasks ->
                        val filteredTasks = tasks.filterNot { task ->
                            val date = task.startDateConf?.dateTime?.toString() ?: ""
                            taskDetailViewModel.isInstanceDateDeleted(date)
                        }
                        DailyView(
                            selectedDate = state.currentDate,
                            onTaskSelected = onTaskSelected,
                            calendarViewModel = calendarViewModel,
                            tasksViewModel = taskListViewModel,
                        )
                    }
                    CalendarView.WEEK -> tasksState?.tasks?.let { tasks ->
                        val filteredTasks = tasks.filterNot { task ->
                            val date = task.startDateConf?.dateTime?.toString() ?: ""
                            taskDetailViewModel.isInstanceDateDeleted(date)
                        }
                        WeeklyView(
                            weekStartDateInput = calendarState!!.currentDate,
                            tasks = filteredTasks,
                            onTaskSelected = onTaskSelected,
                            calendarViewModel = calendarViewModel,
                            tasksViewModel = taskListViewModel,
                        )
                    }
                    CalendarView.MONTH -> tasksState?.tasks?.let { tasks ->
                        val filteredTasks = tasks.filterNot { task ->
                            val date = task.startDateConf?.dateTime?.toString() ?: ""
                            taskDetailViewModel.isInstanceDateDeleted(date)
                        }
                        MonthlyView(
                            selectedMonth = java.time.YearMonth.from(calendarState!!.currentDate),
                            onTaskSelected = onTaskSelected,
                            calendarViewModel = calendarViewModel,
                        )
                    }
                }
            }
        }

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

        selectedTaskId?.let { taskId ->

            val detailViewModel: TaskDetailViewModel =
                koinViewModel(parameters = { parametersOf(taskId, selectedInstanceIdentifier) })

            LaunchedEffect(detailViewModel) {
                detailViewModel.effect.collectLatest { effect ->
                    when (effect) {
                        is TaskDetailEffect.NavigateBack -> {
                            selectedTaskId = null
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

            TaskDetailSheet(
                taskId = taskId,
                instanceIdentifier = selectedInstanceIdentifier,
                onDismiss = { selectedTaskId = null }
            )
        }

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

        if (showAddEditSheet) {

            val editViewModel: TaskEditViewModel =
                koinViewModel(parameters = { parametersOf(taskToEdit?.id ?: 0) })

            LaunchedEffect(editViewModel) {
                editViewModel.effect.collectLatest { effect ->
                    when (effect) {
                        is TaskEditEffect.NavigateBack -> {
                            showAddEditSheet = false
                            if (taskToEdit != null) {
                                selectedTaskId = taskToEdit?.id
                            }
                            taskToEdit = null
                        }

                        is TaskEditEffect.ShowSnackbar -> {
                            snackbarHostState.showSnackbar(effect.message)
                        }
                    }
                }
            }

            LaunchedEffect(taskToEdit?.id) {
                editViewModel.sendIntent(TaskEditIntent.LoadTask(taskToEdit?.id ?: 0))
            }

            ModificationTaskSheet(
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
    onViewSelectorClicked: () -> Unit,
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
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(24.dp),
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
    onSelected: () -> Unit,
) {
    val tintColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .clickable(onClick = onSelected)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(view.getIconRes()),
            contentDescription = view.name,
            tint = tintColor,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = view.name,
            color = tintColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

fun CalendarView.getIconRes(): Int = when (this) {
    CalendarView.DAY -> R.drawable.ic_day_view
    CalendarView.WEEK -> R.drawable.ic_week_view
    CalendarView.MONTH -> R.drawable.ic_month_view
}