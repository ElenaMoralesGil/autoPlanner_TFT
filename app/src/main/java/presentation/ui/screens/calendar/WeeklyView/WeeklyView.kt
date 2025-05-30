package com.elena.autoplanner.presentation.ui.screens.calendar.WeeklyView

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
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
    weekStartDateInput: LocalDate, 
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit,
    calendarViewModel: CalendarViewModel,
    tasksViewModel: TaskListViewModel,
) {
    val hourHeightDp = 60.dp 
    val hourHeightPx = with(LocalDensity.current) { hourHeightDp.toPx() }
    val scrollState = rememberScrollState()
    val currentTime = LocalTime.now()


    val weekStartDate = remember(weekStartDateInput) {
        weekStartDateInput.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    val weekDays = remember(weekStartDate) {
        (0..6).map { weekStartDate.plusDays(it.toLong()) }
    }

    val weekTasks = remember(tasks, weekDays) {
        tasks.filter { task ->
            val relevantDate = task.scheduledStartDateTime?.toLocalDate()
                ?: task.startDateConf?.dateTime?.toLocalDate()
            relevantDate?.let { date ->
                date >= weekDays.first() && date <= weekDays.last()
            } == true
        }
    }


    val allDayTasks = remember(weekTasks) { weekTasks.filter { it.isAllDay() } }
    val scheduledTasks = remember(weekTasks) {
        weekTasks.filter {
            val displayDateTime = it.scheduledStartDateTime ?: it.startDateConf?.dateTime
            displayDateTime != null && !it.isAllDay() && !it.hasPeriod
        }
    }
    val periodTasks = remember(weekTasks) {
        weekTasks.filter {
            it.scheduledStartDateTime == null &&
                    it.startDateConf?.dayPeriod != DayPeriod.NONE &&
                    it.startDateConf?.dayPeriod != DayPeriod.ALLDAY
        }
    }

    val onTaskTimeChanged: (Task, LocalTime, Long) -> Unit = { task, newTime, dayOffset ->
        val originalDateTime = task.scheduledStartDateTime ?: task.startDateConf?.dateTime
        if (originalDateTime != null) {
            val originalDate = originalDateTime.toLocalDate()
            val newDate = originalDate.plusDays(dayOffset) 
            val newDateTime = LocalDateTime.of(newDate, newTime)

            val newConf = TimePlanning(
                dateTime = newDateTime,
                dayPeriod = DayPeriod.NONE 
            )
            val updatedTask = Task.from(task)
                .startDateConf(newConf)
                .scheduledStartDateTime(null)
                .scheduledEndDateTime(null)   
                .build()
            Log.d(
                "WeeklyView",
                "Task ${updatedTask.id} dragged. Updating with StartConf: ${updatedTask.startDateConf}, Cleared Scheduled Times."
            )
            tasksViewModel.sendIntent(TaskListIntent.UpdateTask(updatedTask))
        } else {
            Log.e(
                "WeeklyView",
                "Cannot update task ${task.id} time, original date/time context is missing."
            )
        }
    }

    val morningTasks =
        remember(periodTasks) { periodTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.MORNING } }
    val eveningTasks =
        remember(periodTasks) { periodTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.EVENING } } 
    val nightTasks =
        remember(periodTasks) { periodTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.NIGHT } }

    LaunchedEffect(weekDays) {
        val today = LocalDate.now()
        if (weekDays.any { it == today }) {

            val currentHour = LocalTime.now().hour
            val scrollToPosition = ((currentHour - 1).coerceAtLeast(0) * hourHeightPx).roundToInt()
            scrollState.animateScrollTo(scrollToPosition)
        } else {

            scrollState.animateScrollTo(0)
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




        AllDayTasksSection(
            allDayTasks = allDayTasks,
            weekDays = weekDays,
            onTaskSelected = onTaskSelected
        )


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
                onTaskTimeChanged = onTaskTimeChanged,
                scrollState = scrollState,
                currentTime = currentTime
            )
        }
    }
}

