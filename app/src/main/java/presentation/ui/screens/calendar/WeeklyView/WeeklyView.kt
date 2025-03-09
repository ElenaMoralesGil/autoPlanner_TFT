package com.elena.autoplanner.presentation.ui.screens.calendar.WeeklyView

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

@Composable
fun WeeklyView(
    weekStartDate: LocalDate,
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit,
    calendarViewModel: CalendarViewModel,
    tasksViewModel: TaskListViewModel
) {
    val hourHeightDp = 60.dp
    val hourHeightPx = with(LocalDensity.current) { hourHeightDp.toPx() }
    val scrollState = rememberScrollState()
    val currentTime = LocalTime.now()
    val currentMinutes = currentTime.hour * 60 + currentTime.minute

    val weekDays = remember(weekStartDate) {
        val firstDay = weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        (0..6).map { firstDay.plusDays(it.toLong()) }
    }

    val weekTasks = tasks.filter { task ->
        task.startDateConf?.dateTime?.toLocalDate()?.let { date ->
            weekDays.contains(date)
        } ?: false
    }
    val allDayTasks = weekTasks.filter { it.isAllDay() }
    val scheduledTasks = weekTasks.filter { !it.hasPeriod && !it.isAllDay() }
    val morningTasks = weekTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.MORNING }
    val eveningTasks = weekTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.EVENING }
    val nightTasks = weekTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.NIGHT }


    LaunchedEffect(weekStartDate) {
        if (weekDays.any { it.isToday() }) {
            val scrollToPosition = (currentMinutes / 60f * hourHeightPx).toInt() - 200
            scrollState.animateScrollTo(scrollToPosition.coerceAtLeast(0))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        WeekNavigationHeader(
            weekStartDate = weekDays.first(),
            weekEndDate = weekDays.last(),
            onPreviousWeek = {
                calendarViewModel.sendIntent(
                    CalendarIntent.ChangeDate(weekStartDate.minusWeeks(1))
                )
            },
            onNextWeek = {
                calendarViewModel.sendIntent(
                    CalendarIntent.ChangeDate(weekStartDate.plusWeeks(1))
                )
            }
        )

        DaysOfWeekHeader(
            weekDays = weekDays,
            currentDate = LocalDate.now(),
            onDateSelected = { selectedDate ->
                calendarViewModel.sendIntent(CalendarIntent.ChangeDate(selectedDate))
            }
        )

        Column(modifier = Modifier.weight(1f)) {
            if (allDayTasks.isNotEmpty()) {
                AllDayTasksSection(
                    allDayTasks = allDayTasks,
                    weekDays = weekDays,
                    onTaskSelected = onTaskSelected
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
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
                        val currentDate =
                            task.startDateConf.dateTime?.toLocalDate() ?: LocalDate.now()
                        val newDate = currentDate.plusDays(dayOffset)

                        val updatedTask = task.startDateConf.dayPeriod.let {
                            TimePlanning(
                                dateTime = LocalDateTime.of(newDate, newTime),
                                dayPeriod = it
                            )
                        }.let {
                            Task.from(task).startDateConf(it).build()
                        }
                        tasksViewModel.sendIntent(TaskListIntent.UpdateTask(updatedTask))
                    },
                    scrollState = scrollState,
                    currentTime = currentTime
                )
            }
        }
    }
}
