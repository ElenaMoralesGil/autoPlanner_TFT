package com.elena.autoplanner.presentation.ui.screens.calendar.WeeklyView

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.models.isToday
import com.elena.autoplanner.presentation.intents.CalendarIntent
import com.elena.autoplanner.presentation.intents.TaskListIntent
import com.elena.autoplanner.presentation.viewmodel.CalendarViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskListViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@Composable
fun WeeklyView(
    weekStartDateInput: LocalDate, // The reference date for the week (e.g., selected date)
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit,
    calendarViewModel: CalendarViewModel,
    tasksViewModel: TaskListViewModel,
) {
    val hourHeightDp = 60.dp // Height for one hour slot
    val hourHeightPx = with(LocalDensity.current) { hourHeightDp.toPx() }
    val scrollState = rememberScrollState()
    val currentTime = LocalTime.now()

    // Calculate the actual start of the week (Monday) based on the input date
    val weekStartDate = remember(weekStartDateInput) {
        weekStartDateInput.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    // Calculate the days of the week based on the *actual* start date (Monday)
    val weekDays = remember(weekStartDate) {
        (0..6).map { weekStartDate.plusDays(it.toLong()) }
    }

    // Filter tasks relevant to the displayed week days
    val weekTasks = remember(tasks, weekDays) {
        tasks.filter { task ->
            task.startDateConf?.dateTime?.toLocalDate()?.let { date ->
                date >= weekDays.first() && date <= weekDays.last()
            } ?: false
        }
    }

    // Categorize tasks for different sections
    val allDayTasks = remember(weekTasks) { weekTasks.filter { it.isAllDay() } }
    val scheduledTasks =
        remember(weekTasks) { weekTasks.filter { !it.hasPeriod && !it.isAllDay() && it.startDateConf?.dateTime != null } }
    val morningTasks =
        remember(weekTasks) { weekTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.MORNING } }
    val eveningTasks =
        remember(weekTasks) { weekTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.EVENING } } // Assuming AFTERNOON maps here
    val nightTasks =
        remember(weekTasks) { weekTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.EVENING || it.startDateConf.dayPeriod == DayPeriod.NIGHT } } // Grouping evening/night

    // Scroll to current time on initial composition or when week changes to contain today
    LaunchedEffect(weekDays) {
        val today = LocalDate.now()
        if (weekDays.any { it == today }) {
            // Calculate scroll position slightly above the current hour
            val currentHour = LocalTime.now().hour
            val scrollToPosition = ((currentHour - 1).coerceAtLeast(0) * hourHeightPx).roundToInt()
            scrollState.animateScrollTo(scrollToPosition)
        } else {
            // Optionally scroll to top if today is not in view
            scrollState.animateScrollTo(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Top navigation header (Previous/Next Week, Date Range)
        WeekNavigationHeader(
            weekStartDate = weekDays.first(), // Monday
            weekEndDate = weekDays.last(),   // Sunday
            onPreviousWeek = {
                // Navigate to the Monday of the previous week
                calendarViewModel.sendIntent(
                    CalendarIntent.ChangeDate(weekStartDate.minusWeeks(1))
                )
            },
            onNextWeek = {
                // Navigate to the Monday of the next week
                calendarViewModel.sendIntent(
                    CalendarIntent.ChangeDate(weekStartDate.plusWeeks(1))
                )
            }
        )

        // Header displaying clickable days of the week (MON 1, TUE 2, ...)
        DaysOfWeekHeader(
            weekDays = weekDays,
            currentDate = LocalDate.now(), // Highlight today
            onDateSelected = { selectedDate ->
                // Change view or navigate when a date header is clicked
                calendarViewModel.sendIntent(CalendarIntent.ChangeDate(selectedDate))
                // Consider if you want WeeklyView to switch to DailyView on tap,
                // or just change the highlighted date. Currently changes the focused date.
            }
        )

        // Separator(modifier = Modifier.padding(vertical = 4.dp)) // Optional separator

        // Section for All-Day tasks displayed horizontally
        AllDayTasksSection(
            allDayTasks = allDayTasks,
            weekDays = weekDays,
            onTaskSelected = onTaskSelected
        )

        // The main scrollable area containing period sections and the time grid
        Box(
            modifier = Modifier
                .weight(1f) // Takes remaining vertical space
                .fillMaxWidth()
        ) {
            TimeGridWithPeriodSections(
                weekDays = weekDays,
                scheduledTasks = scheduledTasks,
                morningTasks = morningTasks,
                eveningTasks = eveningTasks,
                nightTasks = nightTasks,
                hourHeightDp = hourHeightDp,
                onTaskSelected = onTaskSelected,
                onTaskTimeChanged = { task, newTime, dayOffset ->
                    val originalDateTime = task.startDateConf?.dateTime
                    if (originalDateTime != null) {
                        val originalDate = originalDateTime.toLocalDate()
                        val newDate = originalDate.plusDays(dayOffset)
                        val newDateTime = LocalDateTime.of(newDate, newTime)

                        val newConf = TimePlanning(
                            dateTime = newDateTime,
                            dayPeriod = DayPeriod.NONE
                        )
                        val updatedTask = Task.from(task).startDateConf(newConf).build()
                        tasksViewModel.sendIntent(TaskListIntent.UpdateTask(updatedTask))
                    }
                },
                scrollState = scrollState,
                currentTime = currentTime
            )
        }
    }
}

